package com.cyprias.chunkspawnerlimiter.configs.impl;

public class WorldLimits {
    private final String name;
    private final int maxY;
    private final int minY;

    public WorldLimits(String name, int maxY, int minY) {
        this.name = name;
        this.maxY = maxY;
        this.minY = minY;
    }

    @Override
    public String toString() {
        return "WorldLimits{" +
                "name='" + name + '\'' +
                ", maxY=" + maxY +
                ", minY=" + minY +
                '}';
    }

    public String getName() {
        return name;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMinY() {
        return minY;
    }
}
