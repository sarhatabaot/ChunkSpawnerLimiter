package com.cyprias.chunkspawnerlimiter.inspection.entities;

import org.bukkit.entity.Entity;

import java.util.List;

public class EntityChunkInspectionResult {
    private final List<Entity> entitiesToRemove;

    public EntityChunkInspectionResult(List<Entity> entitiesToRemove) {
        this.entitiesToRemove = entitiesToRemove;
    }

    public List<Entity> getEntitiesToRemove() {
        return entitiesToRemove;
    }
}
