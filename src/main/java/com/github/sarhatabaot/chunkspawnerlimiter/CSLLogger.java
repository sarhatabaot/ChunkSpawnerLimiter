package com.github.sarhatabaot.chunkspawnerlimiter;


import java.util.logging.Level;
import java.util.logging.Logger;

public final class CSLLogger {
    private static PluginConfig pluginConfig;
    private static final Logger LOGGER = Logger.getLogger("CSL");

    private CSLLogger() {
    }

    public static void setup(PluginConfig pluginConfig) {
        CSLLogger.pluginConfig = pluginConfig;
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    public static void debug(String message) {
        if (pluginConfig.isDebugMessages()) {
            log("DEBUG " + message);
        }
    }

    // --- Core Implementation ---

    private static void log(String message) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        String caller = "UnknownSource";
        if (stack.length > 3) {
            StackTraceElement element = stack[3];
            caller = element.getClassName() + "#" + element.getMethodName() + ":" + element.getLineNumber();
        }

        LOGGER.log(Level.INFO, "[" + caller + "] " + message);
    }

}