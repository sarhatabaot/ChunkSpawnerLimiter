package com.github.sarhatabaot.chunkspawnerlimiter.reflection;
import org.bukkit.World;
import java.lang.reflect.Method;

public final class WorldReflection {

    private static final boolean SUPPORTED;
    private static final Method GET_MIN_HEIGHT;

    static {
        Method getMinHeight = null;
        boolean supported;

        try {
            // Try to find the method (MC 1.18+)
            getMinHeight = World.class.getMethod("getMinHeight");
            supported = true;
        } catch (NoSuchMethodException e) {
            // Older version (like 1.8.8) — method doesn't exist
            supported = false;
        }

        GET_MIN_HEIGHT = getMinHeight;
        SUPPORTED = supported;
    }

    private WorldReflection() {}

    /**
     * @return true if this server version supports getMinHeight().
     */
    public static boolean isSupported() {
        return SUPPORTED;
    }

    /**
     * Returns the world's minimum height safely across all versions.
     */
    public static int getWorldMinHeightSafe(World world) {
        if (!SUPPORTED) {
            // Old versions (pre-1.18) start at Y = 0
            return 0;
        }

        try {
            return (int) GET_MIN_HEIGHT.invoke(world);
        } catch (ReflectiveOperationException e) {
            // Fallback if reflection fails for any reason
            e.printStackTrace();
            return 0;
        }
    }
}
