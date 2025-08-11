package org.overb.jsontocsv.dto;

import org.overb.jsontocsv.App;

public class AppVersion implements Comparable<AppVersion> {

    private static String currentVersion;
    private final int major;
    private final int minor;
    private final int patch;

    public AppVersion(String version) {
        version = normalize(version);
        String[] parts = version.split("\\.");
        major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
    }

    private static String normalize(String v) {
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        int i = 0;
        while (i < v.length()) {
            char c = v.charAt(i);
            if (!(c == '.' || (c >= '0' && c <= '9'))) break;
            i++;
        }
        return i == 0 ? "0" : v.substring(0, i);
    }

    public static String getCurrentVersion() {
        if (currentVersion == null) {
            String version = App.class.getPackage() != null ? App.class.getPackage().getImplementationVersion() : null;
            currentVersion = "v" + (version == null ? "Dev" : version);
        }
        return currentVersion;
    }

    @Override
    public int compareTo(AppVersion o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
