package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.overb.jsontocsv.dto.UpdateStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpService {

    private final HttpClient client;
    private final Duration requestTimeout;
    private final int maxRetries;
    private final Duration initialBackoff;
    private final Map<String, String> defaultHeaders;

    public HttpService() {
        this(Duration.ofSeconds(10), Duration.ofSeconds(30), 3, Duration.ofMillis(400),
                "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");
    }

    public HttpService(Duration connectTimeout, Duration requestTimeout, int maxRetries, Duration initialBackoff, String userAgent) {
        this.requestTimeout = requestTimeout;
        this.maxRetries = Math.max(0, maxRetries);
        this.initialBackoff = initialBackoff;

        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(connectTimeout)
                .build();

        this.defaultHeaders = Map.of(
                "User-Agent", userAgent,
                "Accept-Encoding", "gzip, deflate",
                "Accept", "*/*"
        );
    }

    public UpdateStatus fetchUpdateStatus(String url) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .GET()
                .header("Accept", "application/json");
        defaultHeaders.forEach(b::header);
        HttpRequest request = b.build();
        String body = sendWithRetries(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return parseUpdateStatus(body);
    }

    public String getText(String url) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).GET().timeout(requestTimeout);
        defaultHeaders.forEach(b::header);
        return sendWithRetries(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public String postJson(String url, String json) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json");
        defaultHeaders.forEach(b::header);
        return sendWithRetries(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public Path downloadToDownloads(String url) throws IOException, InterruptedException {
        return downloadTo(url, getDefaultDownloadsFolder());
    }

    public Path downloadTo(String url, Path targetDir) throws IOException, InterruptedException {
        Files.createDirectories(targetDir);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).GET().timeout(requestTimeout).header("Accept", "*/*");
        defaultHeaders.forEach(b::header);
        HttpResponse<InputStream> response = sendWithRetries(b.build(), HttpResponse.BodyHandlers.ofInputStream(), true);

        if (response.statusCode() / 100 != 2) {
            try (InputStream is = response.body()) {
                is.readAllBytes();
            }
            throw new IOException("Download failed with HTTP " + response.statusCode());
        }

        String fileName = deriveFilename(url, response.headers());
        Path target = uniquePath(targetDir.resolve(fileName));
        try (InputStream in = response.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private <T> T sendWithRetries(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        HttpResponse<T> response = sendWithRetries(request, handler, false);
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private <T> HttpResponse<T> sendWithRetries(HttpRequest request, HttpResponse.BodyHandler<T> handler, boolean streamResponse) throws IOException, InterruptedException {
        Duration backoff = initialBackoff;
        IOException lastIo = null;
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                HttpResponse<T> resp = client.send(request, handler);
                if (!isRetryableStatus(resp.statusCode())) {
                    return resp;
                }
            } catch (HttpTimeoutException | ConnectException e) {
                lastIo = e;
            } catch (IOException e) {
                lastIo = e;
            }
            if (attempt > maxRetries) break;
            sleepWithJitter(backoff);
            backoff = backoff.multipliedBy(2);
        }
        if (lastIo != null) throw lastIo;
        throw new IOException("Failed to send request and no response was returned");
    }

    private static final Pattern FILENAME_STAR = Pattern.compile("filename\\*=(?:UTF-8''|\")?([^;\"]+)");
    private static final Pattern FILENAME = Pattern.compile("filename=\"?([^;\"]+)\"?");

    private static String deriveFilename(String url, HttpHeaders headers) {
        Optional<String> cd = headers.firstValue("Content-Disposition");
        if (cd.isPresent()) {
            String v = cd.get();
            Matcher mStar = FILENAME_STAR.matcher(v);
            if (mStar.find()) return rfc5987Decode(mStar.group(1)).strip();
            Matcher m = FILENAME.matcher(v);
            if (m.find()) return sanitizeFilename(m.group(1).strip());
        }
        String path = URI.create(url).getPath();
        if (path != null && !path.isEmpty()) {
            int slash = path.lastIndexOf('/') + 1;
            if (slash >= 0 && slash < path.length()) {
                String name = path.substring(slash);
                if (!name.isBlank()) return sanitizeFilename(name);
            }
        }
        return "download";
    }

    private static String rfc5987Decode(String value) {
        try {
            String s = value.replace("+", "%2B");
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return sanitizeFilename(value);
        }
    }

    private static String sanitizeFilename(String name) {
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        cleaned = cleaned.replaceAll("[\\p{Cntrl}]", "");
        if (cleaned.isBlank()) return "download";
        return cleaned;
    }

    private static Path uniquePath(Path base) throws IOException {
        if (!Files.exists(base)) return base;
        String fileName = base.getFileName().toString();
        String stem;
        String ext;
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            stem = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        } else {
            stem = fileName;
            ext = "";
        }
        for (int i = 1; i < 10_000; i++) {
            Path p = base.getParent().resolve(stem + " (" + i + ")" + ext);
            if (!Files.exists(p)) return p;
        }
        throw new IOException("Could not find unique filename for " + base);
    }

    private static Path getDefaultDownloadsFolder() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        Path home = Paths.get(System.getProperty("user.home"));
        if (os.contains("win") || os.contains("mac")) return home.resolve("Downloads");
        Path xdg = home.resolve(".config/user-dirs.dirs");
        if (Files.isRegularFile(xdg)) {
            try {
                for (String line : Files.readAllLines(xdg, StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (line.startsWith("XDG_DOWNLOAD_DIR")) {
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String val = line.substring(idx + 1).trim();
                            val = val.replaceAll("^\"|\"$", "");
                            val = val.replace("$HOME", home.toString());
                            return Paths.get(val);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return home.resolve("Downloads");
    }

    private static boolean isRetryableStatus(int code) {
        return (code >= 500 && code <= 599) || code == 408 || code == 429;
    }

    private static void sleepWithJitter(Duration base) throws InterruptedException {
        long jitterMillis = ThreadLocalRandom.current().nextLong(50, 150);
        long sleepMillis = Math.min(base.toMillis() + jitterMillis, 10_000);
        Thread.sleep(sleepMillis);
    }

    private static UpdateStatus parseUpdateStatus(String body) throws JsonProcessingException {
        return JsonIo.MAPPER.readValue(body, UpdateStatus.class);
    }
}