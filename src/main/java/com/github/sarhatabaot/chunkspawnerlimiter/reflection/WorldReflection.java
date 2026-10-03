package com.github.sarhatabaot.chunkspawnerlimiter.reflection;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class WorldReflection {

    private static final Method GET_MIN_HEIGHT;
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();
    private static volatile boolean available;

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
        available = supported;
    }

    private WorldReflection() {}

    /**
     * @return true if this server version supports getMinHeight().
     */
    public static boolean isSupported() {
        return available;
    }

    /**
     * Returns the world's minimum height safely across all versions.
     */
    public static int getWorldMinHeightSafe(World world) {
        if (!available) {
            // Old versions (pre-1.18) start at Y = 0
            return 0;
        }

        try {
            return (int) GET_MIN_HEIGHT.invoke(world);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            available = false;
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                Bukkit.getLogger().log(Level.WARNING,
                        "[WorldReflection] Unable to read minimum world height; "
                                + "disabling reflection and falling back to Y=0: "
                                + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
            return 0;
        }
    }
}
