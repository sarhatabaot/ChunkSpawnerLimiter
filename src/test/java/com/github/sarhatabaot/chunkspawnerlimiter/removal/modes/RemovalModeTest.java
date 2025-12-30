package com.github.sarhatabaot.chunkspawnerlimiter.removal.modes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for RemovalMode registry and factory methods.
 * Tests the sealed interface and its implementations.
 */
@DisplayName("RemovalMode Tests")
class RemovalModeTest {

    @BeforeEach
    void setUp() {
        // Ensure clean state for each test
        RemovalMode.reload(null);
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        RemovalMode.reload(null);
    }

    @Test
    @DisplayName("Should register all removal modes correctly")
    void shouldRegisterAllRemovalModesCorrectly() {
        // When - reload is called in setUp

        // Then
        assertThat(RemovalMode.fromString("prevent")).isInstanceOf(Prevent.class);
        assertThat(RemovalMode.fromString("remove")).isInstanceOf(Remove.class);
        assertThat(RemovalMode.fromString("kill")).isInstanceOf(Kill.class);
        assertThat(RemovalMode.fromString("enforce")).isInstanceOf(Enforce.class);
        assertThat(RemovalMode.fromString("enforce-kill")).isInstanceOf(EnforceKill.class);
    }

    @Test
    @DisplayName("Should return enforce mode for unknown mode strings")
    void shouldReturnEnforceModeForUnknownModeStrings() {
        // When
        RemovalMode unknownMode = RemovalMode.fromString("nonexistent");

        // Then
        assertThat(unknownMode).isInstanceOf(Enforce.class);
        assertThat(unknownMode.getKey()).isEqualTo("enforce");
    }

    @Test
    @DisplayName("Should return enforce mode for null input")
    void shouldReturnEnforceModeForNullInput() {
        // When
        RemovalMode nullMode = RemovalMode.fromString(null);

        // Then
        assertThat(nullMode).isInstanceOf(Enforce.class);
        assertThat(nullMode.getKey()).isEqualTo("enforce");
    }

    @Test
    @DisplayName("Should handle case insensitive mode resolution")
    void shouldHandleCaseInsensitiveModeResolution() {
        // When
        RemovalMode upperCase = RemovalMode.fromString("PREVENT");
        RemovalMode mixedCase = RemovalMode.fromString("ReMoVe");
        RemovalMode lowerCase = RemovalMode.fromString("kill");

        // Then
        assertThat(upperCase).isInstanceOf(Prevent.class);
        assertThat(mixedCase).isInstanceOf(Remove.class);
        assertThat(lowerCase).isInstanceOf(Kill.class);
    }

    @Test
    @DisplayName("Should have correct keys for all modes")
    void shouldHaveCorrectKeysForAllModes() {
        // When
        RemovalMode prevent = RemovalMode.fromString("prevent");
        RemovalMode remove = RemovalMode.fromString("remove");
        RemovalMode kill = RemovalMode.fromString("kill");
        RemovalMode enforce = RemovalMode.fromString("enforce");
        RemovalMode enforceKill = RemovalMode.fromString("enforce-kill");

        // Then
        assertThat(prevent.getKey()).isEqualTo("prevent");
        assertThat(remove.getKey()).isEqualTo("remove");
        assertThat(kill.getKey()).isEqualTo("kill");
        assertThat(enforce.getKey()).isEqualTo("enforce");
        assertThat(enforceKill.getKey()).isEqualTo("enforce-kill");
    }

    @Test
    @DisplayName("Should maintain consistent mode instances")
    void shouldMaintainConsistentModeInstances() {
        // When - Getting the same mode multiple times
        RemovalMode prevent1 = RemovalMode.fromString("prevent");
        RemovalMode prevent2 = RemovalMode.fromString("prevent");
        RemovalMode prevent3 = RemovalMode.fromString("PREVENT");

        // Then - Should be the same instance (singleton pattern)
        assertThat(prevent1).isSameAs(prevent2);
        assertThat(prevent2).isSameAs(prevent3);
    }

    @Test
    @DisplayName("Should have all expected removal modes registered")
    void shouldHaveAllExpectedRemovalModesRegistered() {
        // Given
        String[] expectedModes = {"prevent", "remove", "kill", "enforce", "enforce-kill"};

        // Then
        for (String mode : expectedModes) {
            assertThat(RemovalMode.fromString(mode))
                .isNotNull()
                .extracting("key")
                .isEqualTo(mode);
        }
    }

    @Test
    @DisplayName("Should handle setup and reload operations")
    void shouldHandleSetupAndReloadOperations() {
        // Given
        RemovalMode.setup(null); // Should work without task manager

        // When
        RemovalMode reloadMode = RemovalMode.fromString("prevent");

        // Then
        assertThat(reloadMode).isInstanceOf(Prevent.class);

        // When - Reload again
        RemovalMode.reload(null);
        RemovalMode reloadMode2 = RemovalMode.fromString("prevent");

        // Then - Should still work
        assertThat(reloadMode2).isInstanceOf(Prevent.class);
    }
}
