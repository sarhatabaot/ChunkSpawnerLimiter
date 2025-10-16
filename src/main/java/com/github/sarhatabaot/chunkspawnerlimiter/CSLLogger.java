package com.github.sarhatabaot.chunkspawnerlimiter;


import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class CSLLogger {
    private static PluginConfig pluginConfig;
    private static final Logger LOGGER = Logger.getLogger("CSL");

    private CSLLogger() {
    }

    public static void init(PluginConfig pluginConfig) {
        CSLLogger.pluginConfig = pluginConfig;
    }

    public static Logger get() {
        return LOGGER;
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void warn(String message) {
        log(Level.WARNING, message);
    }

    public static void error(String message) {
        log(Level.SEVERE, message);
    }

    public static void debug(String message) {
        if (pluginConfig.isDebugMessages()) {
            log(Level.INFO, "DEBUG " + message);
        }
    }

    // --- Core Implementation ---

    private static void log(Level level, String message) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        // Index [3] = the caller of the public log method
        // Example: LoggerProvider.info() → actual caller
        String caller = "UnknownSource";
        if (stack.length > 3) {
            StackTraceElement element = stack[3];
            caller = element.getClassName() + "#" + element.getMethodName() + ":" + element.getLineNumber();
        }

        LOGGER.log(level, "[" + caller + "] " + message);
    }

}