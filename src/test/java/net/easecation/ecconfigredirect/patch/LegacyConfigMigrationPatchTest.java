package net.easecation.ecconfigredirect.patch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyConfigMigrationPatchTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesPlayerConfigsWithoutCopyingLauncherControlledFiles() throws Exception {
        Path legacy = temporaryDirectory.resolve("config");
        Path target = temporaryDirectory.resolve("ec-config");
        Files.createDirectories(legacy.resolve("bedrock-loader/plugins"));
        Files.createDirectories(legacy.resolve("ecclientsettings/profiles"));
        Files.writeString(legacy.resolve("fml.toml"), "launcher-controlled");
        Files.writeString(legacy.resolve("bedrock-loader/client.yml"), "launcher-controlled");
        Files.writeString(legacy.resolve("bedrock-loader/plugins/example.jar"), "launcher-controlled");
        Files.writeString(legacy.resolve("sodium-options.json"), "player-value");
        Files.writeString(legacy.resolve("ecclientsettings/profiles/default.json"), "player-profile");

        int migrated = LegacyConfigMigrationPatch.migrateMissingFiles(legacy, target);

        assertEquals(2, migrated);
        assertEquals("player-value", Files.readString(target.resolve("sodium-options.json")));
        assertEquals(
                "player-profile",
                Files.readString(target.resolve("ecclientsettings/profiles/default.json")));
        assertFalse(Files.exists(target.resolve("fml.toml")));
        assertFalse(Files.exists(target.resolve("bedrock-loader")));
    }

    @Test
    void neverOverwritesAnExistingPersistentConfig() throws Exception {
        Path legacy = temporaryDirectory.resolve("config");
        Path target = temporaryDirectory.resolve("ec-config");
        Files.createDirectories(legacy);
        Files.createDirectories(target);
        Files.writeString(legacy.resolve("iris.properties"), "temporary-default");
        Files.writeString(target.resolve("iris.properties"), "player-value");

        int migrated = LegacyConfigMigrationPatch.migrateMissingFiles(legacy, target);

        assertEquals(0, migrated);
        assertEquals("player-value", Files.readString(target.resolve("iris.properties")));
    }
}

