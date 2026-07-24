package net.easecation.ecconfigredirect.patch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResourcePackDirectoryPatchTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void redirectsOnlyTheDefaultResourcePackDirectory() throws Exception {
        Path defaultDirectory = temporaryDirectory.resolve("resourcepacks");
        Path customDirectory = temporaryDirectory.resolve("launcher-controlled-packs");

        assertEquals(
                temporaryDirectory.resolve("ec-resourcepacks").toAbsolutePath().normalize(),
                ResourcePackDirectoryPatch.resolveDirectory(temporaryDirectory, defaultDirectory));
        assertEquals(
                customDirectory.toAbsolutePath().normalize(),
                ResourcePackDirectoryPatch.resolveDirectory(temporaryDirectory, customDirectory));
    }

    @Test
    void migratesLegacyPacksWithoutOverwritingPersistentFiles() throws Exception {
        Path legacyDirectory = temporaryDirectory.resolve("resourcepacks");
        Path persistentDirectory = temporaryDirectory.resolve("ec-resourcepacks");
        Files.createDirectories(legacyDirectory.resolve("folder-pack"));
        Files.createDirectories(persistentDirectory.resolve("folder-pack"));
        Files.writeString(legacyDirectory.resolve("example.zip"), "legacy zip");
        Files.writeString(legacyDirectory.resolve("folder-pack/pack.mcmeta"), "legacy metadata");
        Files.writeString(persistentDirectory.resolve("folder-pack/pack.mcmeta"), "player metadata");

        int migrated = ResourcePackDirectoryPatch.migrateMissingFiles(
                legacyDirectory, persistentDirectory);

        assertEquals(1, migrated);
        assertEquals("legacy zip", Files.readString(persistentDirectory.resolve("example.zip")));
        assertEquals(
                "player metadata",
                Files.readString(persistentDirectory.resolve("folder-pack/pack.mcmeta")));
        assertFalse(Files.exists(persistentDirectory.resolve("resourcepacks")));
    }
}
