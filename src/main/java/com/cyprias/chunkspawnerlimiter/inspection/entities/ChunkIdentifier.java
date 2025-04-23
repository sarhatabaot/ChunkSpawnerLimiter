package com.cyprias.chunkspawnerlimiter.inspection.entities;

import java.util.Objects;
import java.util.UUID;

public class ChunkIdentifier {
    private final UUID worldUuid;
    private final int x;
    private final int z;

    public ChunkIdentifier(UUID worldUuid, int x, int z) {
        this.worldUuid = worldUuid;
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkIdentifier that = (ChunkIdentifier) o;
        return x == that.x && z == that.z && worldUuid.equals(that.worldUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldUuid, x, z);
    }

    public UUID getWorldUuid() {
        return worldUuid;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "ChunkIdentifier{" +
                "worldUuid=" + worldUuid +
                ", x=" + x +
                ", z=" + z +
                '}';
    }
}
