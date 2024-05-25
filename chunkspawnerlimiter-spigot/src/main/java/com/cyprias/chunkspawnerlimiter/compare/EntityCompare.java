package com.cyprias.chunkspawnerlimiter.compare;

import org.bukkit.entity.Entity;

public interface EntityCompare {
    /**
     * Evaluates the given Entity against the specific criteria as defined by the implementing class.
     *
     * @param entity The entity to evaluate.
     * @return Returns true if the entity is found to be similar.
     */
    boolean isSimilar(Entity entity);
}
