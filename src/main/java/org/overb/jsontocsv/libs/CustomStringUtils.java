package org.overb.jsontocsv.libs;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomStringUtils {

    private static final Pattern UNESCAPED_NEWLINE = Pattern.compile("(?<!\\\\)\\n");
    private static final Pattern UNESCAPED_CR = Pattern.compile("(?<!\\\\)\\r");

    public static String makePrettySize(Long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return "0B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(sizeInBytes / Math.pow(1024, digitGroups)) + units[digitGroups];
    }

    public static String makePrettyDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        }
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        milliseconds -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        milliseconds -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        milliseconds -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d");
        if (hours > 0 || sb.length() > 0) sb.append(hours).append("h");
        if (minutes > 0 || sb.length() > 0) sb.append(minutes).append("m");
        if (seconds > 0 || sb.length() > 0) sb.append(seconds).append("s");
        sb.append(milliseconds).append("ms");

        return sb.toString();
    }

    public static String generateColumnName(String original) {
        if (original.startsWith("\"") && original.endsWith("\"")) {
            original = original.substring(1, original.length() - 1);
        }
        //attempt to transform any string into a snake case string
        if (isAlphanumeric(original)) {
            return camelToSnake(original);
        }
        String fixed = original.replaceAll("[^a-zA-Z0-9]", "_");
        while (fixed.startsWith("_")) {
            fixed = fixed.substring(1);
        }
        while (fixed.contains("__")) {
            fixed = fixed.replace("__", "_");
        }
        while (fixed.endsWith("_")) {
            fixed = fixed.substring(0, fixed.length() - 1);
        }
        return fixed.toLowerCase();
    }

    public static String camelToSnake(String str) {
        if (str == null) {
            return null;
        }
        str = str.trim();
        if (str.length() < 2) {
            return str.toLowerCase();
        }
        StringBuilder result = new StringBuilder(str.length() * 2);
        char ch = str.charAt(0);
        boolean previousUppercase = Character.isUpperCase(ch);
        result.append(Character.toLowerCase(ch));
        for (int i = 1; i < str.length(); i++) {
            ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (!previousUppercase) {
                    result.append("_");
                }
                result.append(Character.toLowerCase(ch));
                previousUppercase = true;
            } else {
                result.append(ch);
                previousUppercase = false;
            }
        }
        return result.toString().toLowerCase();
    }

    private static boolean isAlphanumeric(final CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetterOrDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String encodeUtf8(CharSequence input) {
        //inspired by https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/translate/UnicodeUnescaper.html
        if (input == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(input.length() * 2);
        int pos = 0;
        final int len = input.length();
        while (pos < len) {
            pos += translateCharacter(input, pos, sb);
        }
        return sb.toString();
    }

    private static int translateCharacter(CharSequence input, int index, StringBuilder out) {
        if (input.charAt(index) == '\\' && index + 1 < input.length() && input.charAt(index + 1) == 'u') {
            if (index > 0 && input.charAt(index - 1) == '\\') {
                out.append("\\u");
                return 2;
            }
            int i = 2;
            while (index + i < input.length() && input.charAt(index + i) == 'u') {
                i++;
            }
            if (index + i < input.length() && input.charAt(index + i) == '+') {
                i++;
            }
            if (index + i + 4 <= input.length()) {
                final String unicode = input.subSequence(index + i, index + i + 4).toString();
                if (unicode.matches("[0-9a-fA-F]{4}")) {
                    try {
                        out.append((char) Integer.parseInt(unicode, 16));
                        return i + 4;
                    } catch (NumberFormatException ignored) {
                        // normal text
                    }
                }
            }
        }
        out.append(input.charAt(index));
        return 1;
    }

    public static String escapeCsv(String original) {
        return encodeUtf8(escapeString(original, true));
    }

    public static String escapeJson(String original) {
        return encodeUtf8(escapeString(original, false));
    }

    private static String escapeString(String str, boolean useDoubleQuotes) {
        if (str == null) {
            return null;
        }
        str = UNESCAPED_NEWLINE.matcher(str).replaceAll("\\\\n");
        str = UNESCAPED_CR.matcher(str).replaceAll("\\\\r");
        StringBuilder result = new StringBuilder(Math.max(str.length() * 2, 16));
        for (char ch : str.toCharArray()) {
            if (ch < 32) {
                // Handle control characters
                switch (ch) {
                    case '\b':
                        result.append("\\b");
                        break;
                    case '\n':
                        result.append("\\n");
                        break;
                    case '\t':
                        result.append("\\t");
                        break;
                    case '\f':
                        result.append("\\f");
                        break;
                    case '\r':
                        result.append("\\r");
                        break;
                    default:
                        result.append("\\u").append(String.format("%04x", (int) ch));
                        break;
                }
            } else if (ch == '"') {
                result.append(useDoubleQuotes ? "\"\"" : "\\\"");
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static boolean containsWholeWord(String text, String word) {
        // Regex to match whole words (delimited by non-alphanumeric characters or spaces)
        String regex = "\\b" + Pattern.quote(word) + "\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }
}
