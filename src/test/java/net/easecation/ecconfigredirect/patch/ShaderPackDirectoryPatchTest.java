package net.easecation.ecconfigredirect.patch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderPackDirectoryPatchTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsPersistentDirectoryAndMigratesWithoutOverwritingPlayers() throws Exception {
        Path legacyDirectory = temporaryDirectory.resolve("shaderpacks");
        Path persistentDirectory = temporaryDirectory.resolve("ec-shaderpacks");
        Files.createDirectories(legacyDirectory.resolve("folder-shader"));
        Files.createDirectories(persistentDirectory.resolve("folder-shader"));
        Files.writeString(legacyDirectory.resolve("example.zip"), "legacy zip");
        Files.writeString(legacyDirectory.resolve("folder-shader/shaders.properties"), "legacy");
        Files.writeString(persistentDirectory.resolve("folder-shader/shaders.properties"), "player");

        ShaderPackDirectoryPatch.apply(new ConfigPatchContext(
                temporaryDirectory,
                temporaryDirectory.resolve("config"),
                temporaryDirectory.resolve("ec-config"),
                Set.of("iris")));

        assertEquals("legacy zip", Files.readString(persistentDirectory.resolve("example.zip")));
        assertEquals("player", Files.readString(persistentDirectory.resolve("folder-shader/shaders.properties")));
    }

    @Test
    void registrySkipsMigrationWhenIrisIsAbsent() {
        ConfigPatchDefinition definition = ConfigPatchRegistry.PATCHES.stream()
                .filter(patch -> patch.id().equals("migrate-legacy-iris-shader-packs"))
                .findFirst()
                .orElseThrow();
        ConfigPatchContext context = new ConfigPatchContext(
                temporaryDirectory,
                temporaryDirectory.resolve("config"),
                temporaryDirectory.resolve("ec-config"),
                Set.of());

        assertFalse(definition.appliesTo(context));
        assertTrue(definition.requiredModIds().contains("iris"));
    }
}
