package com.github.sarhatabaot.chunkspawnerlimiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReflectionTests {

    @Test
    public void testExtractVersionFromPackage() {
        String pkg = "org.bukkit.craftbukkit.v1_20_R3";
        String[] parts = pkg.split("\\.");

        assertEquals("v1_20_R3", parts[3]);

        String className = "org.bukkit.craftbukkit." + parts[3] + ".CraftWorld";
        assertEquals("org.bukkit.craftbukkit.v1_20_R3.CraftWorld", className);
    }

}
