package net.easecation.ecconfigredirect.patch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SodiumMixinConfigPatchTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void linksHardcodedLegacyPathToThePersistentCopy() throws Exception {
        Path legacy = temporaryDirectory.resolve("config");
        Path target = temporaryDirectory.resolve("ec-config");
        Files.createDirectories(legacy);
        Files.createDirectories(target);
        Files.writeString(legacy.resolve("sodium-mixins.properties"), "launcher-default");
        Files.writeString(target.resolve("sodium-mixins.properties"), "player-value");

        SodiumMixinConfigPatch.bridgeHardcodedPath(legacy, target);

        Path legacyFile = legacy.resolve("sodium-mixins.properties");
        Path persistentFile = target.resolve("sodium-mixins.properties");
        assertTrue(Files.isSameFile(legacyFile, persistentFile));
        assertEquals("player-value", Files.readString(legacyFile));
    }
}

