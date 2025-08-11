package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.overb.jsontocsv.dto.UpdateStatus;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

public final class HttpService {

    private final HttpClient client;
    private final int maxRetries;
    private final Duration requestTimeout;
    private final Duration initialBackoff;
    private final Duration connectTimeout;

    public HttpService() {
        this(3, Duration.ofSeconds(5), Duration.ofMillis(250), Duration.ofSeconds(5));
    }

    public HttpService(int maxRetries, Duration requestTimeout, Duration initialBackoff, Duration connectTimeout) {
        if (maxRetries < 1) throw new IllegalArgumentException("maxRetries must be >= 1");
        this.maxRetries = maxRetries;
        this.requestTimeout = requestTimeout;
        this.initialBackoff = initialBackoff;
        this.connectTimeout = connectTimeout;

        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(connectTimeout)
                .build();
    }

    public UpdateStatus fetchUpdateStatus(String url) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .GET()
                .header("Accept", "application/json")
                .build();

        Duration backoff = initialBackoff;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();

                if (code == 200) {
                    return parseUpdateStatus(response.body());
                }

                if (isRetryableStatus(code) && attempt < maxRetries) {
                    sleepWithJitter(backoff);
                    backoff = backoff.multipliedBy(2);
                    continue;
                }

                throw new IOException("Request failed with HTTP " + code + " for " + uri);
            } catch (HttpTimeoutException | ConnectException e) {
                if (attempt < maxRetries) {
                    sleepWithJitter(backoff);
                    backoff = backoff.multipliedBy(2);
                    continue;
                }
                throw e;
            } catch (IOException e) {
                // Treat other I/O errors as retryable unless it's the last attempt
                if (attempt < maxRetries) {
                    sleepWithJitter(backoff);
                    backoff = backoff.multipliedBy(2);
                    continue;
                }
                throw e;
            }
        }
        throw new IOException("Unreachable: exhausted retries");
    }

    public CompletableFuture<UpdateStatus> fetchUpdateStatusAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchUpdateStatus(url);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        });
    }

    private static boolean isRetryableStatus(int code) {
        // Server-side errors and request timeout
        return (code >= 500 && code <= 599) || code == 408;
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