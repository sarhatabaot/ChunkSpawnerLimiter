package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.util;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for detecting and comparing Minecraft server versions.
 * Supports version detection from Bukkit.getVersion() and package names.
 */
public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private MinecraftVersion(int major, int minor, int patch, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw;
    }

    /**
     * Detect the current Minecraft version from the running server.
     */
    public static MinecraftVersion detect() {
        try {
            String version = Bukkit.getVersion();
            return parse(version);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MinecraftVersion] Failed to detect version: " + e.getMessage());
            return new MinecraftVersion(1, 0, 0, "unknown");
        }
    }

    /**
     * Parse a version string like "1.20.4" or "git-Paper-448 (MC: 1.20.4)"
     */
    public static MinecraftVersion parse(String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            return new MinecraftVersion(1, 0, 0, versionString);
        }

        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new MinecraftVersion(major, minor, patch, versionString);
        }

        return new MinecraftVersion(1, 0, 0, versionString);
    }

    /**
     * Check if this version is at least the specified version.
     */
    public boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    /**
     * Check if this version is at least the specified version.
     */
    public boolean isAtLeast(int major, int minor, int patch) {
        if (this.major > major) return true;
        if (this.major < major) return false;

        if (this.minor > minor) return true;
        if (this.minor < minor) return false;

        return this.patch >= patch;
    }

    /**
     * Check if this version is between min (inclusive) and max (exclusive).
     */
    public boolean isBetween(MinecraftVersion min, MinecraftVersion max) {
        return this.compareTo(min) >= 0 && this.compareTo(max) < 0;
    }

    /**
     * Check if this is a modern version (1.17+) with Mojang mappings.
     */
    public boolean isModern() {
        return isAtLeast(1, 17);
    }

    /**
     * Check if this is a Spigot-era version (1.13-1.16) with version suffixes.
     */
    public boolean isSpigot() {
        return isAtLeast(1, 13) && !isAtLeast(1, 17);
    }

    /**
     * Check if this is a legacy version (1.8.8-1.12) with old NMS structure.
     */
    public boolean isLegacy() {
        return isAtLeast(1, 8, 8) && !isAtLeast(1, 13);
    }

    /**
     * Get the version suffix from the CraftBukkit package (e.g., "v1_20_R3").
     * Returns null if not in legacy/spigot format.
     */
    public static String detectVersionSuffix() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            // e.g. "org.bukkit.craftbukkit.v1_20_R3"
            
            int index = packageName.indexOf("craftbukkit");
            if (index != -1) {
                String after = packageName.substring(index + "craftbukkit".length());
                if (after.startsWith(".")) {
                    after = after.substring(1);
                }
                if (!after.isEmpty() && after.startsWith("v")) {
                    return after;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getRaw() {
        return raw;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + (patch > 0 ? "." + patch : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinecraftVersion)) return false;
        MinecraftVersion that = (MinecraftVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }
}
