package com.cyprias.chunkspawnerlimiter.messages;

public final class Debug {
    public static final String IGNORE_ENTITY = "Ignoring %s due spawn-reason: %s";
    public static final String REMOVING_ENTITY_AT = "Removing %d %s @ %dx %dz";
    public static final String ACTIVE_CHECK = "Active check @ %dx %dz";
    public static final String REGISTER_LISTENERS = "Registered listeners.";
    public static final String CHUNK_UNLOAD_EVENT = "ChunkUnloadEvent %s %s";
    public static final String CHUNK_LOAD_EVENT = "ChunkLoadEvent %s %s";
    public static final String BLOCK_PLACE_CHECK = "Material=%s, Count=%d, Limit=%d";

    private Debug() {
        throw new UnsupportedOperationException("This operation is not supported");
    }
}