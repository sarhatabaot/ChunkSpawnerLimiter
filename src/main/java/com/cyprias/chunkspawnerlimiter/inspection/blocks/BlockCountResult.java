package com.cyprias.chunkspawnerlimiter.inspection.blocks;



public class BlockCountResult {
    private final boolean isWithinLimit;
    private final int count;

    public BlockCountResult(boolean isWithinLimit, int count) {
        this.isWithinLimit = isWithinLimit;
        this.count = count;
    }

    public boolean isWithinLimit() {
        return isWithinLimit;
    }

    public int getCount() {
        return count;
    }
}
