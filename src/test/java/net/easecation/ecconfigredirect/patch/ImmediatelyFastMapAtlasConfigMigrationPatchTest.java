package net.easecation.ecconfigredirect.patch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmediatelyFastMapAtlasConfigMigrationPatchTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesExistingEnabledConfigAndRecordsVersion() throws Exception {
        ConfigPatchContext context = context();
        writeConfig(context, """
                {
                  "map_atlas_generation": true,
                  "fast_text_lookup": false
                }
                """);

        ImmediatelyFastMapAtlasConfigMigrationPatch.apply(context);

        JsonObject migrated = readConfig(context);
        assertFalse(migrated.get("map_atlas_generation").getAsBoolean());
        assertFalse(migrated.get("fast_text_lookup").getAsBoolean());
        Path marker = ImmediatelyFastMapAtlasConfigMigrationPatch.markerFile(
                context.persistentConfigDirectory());
        assertTrue(Files.isRegularFile(marker));
        assertEquals(
                "migration=" + ImmediatelyFastMapAtlasConfigMigrationPatch.MIGRATION_ID,
                Files.readString(marker).trim());
    }

    @Test
    void alreadyMigratedConfigIsNeverRewritten() throws Exception {
        ConfigPatchContext context = context();
        String playerConfig = "{\"map_atlas_generation\":true,\"player_value\":17}";
        writeConfig(context, playerConfig);
        Path marker = ImmediatelyFastMapAtlasConfigMigrationPatch.markerFile(
                context.persistentConfigDirectory());
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "migration=already-applied\n");

        ImmediatelyFastMapAtlasConfigMigrationPatch.apply(context);

        assertEquals(playerConfig, Files.readString(configFile(context)));
    }

    @Test
    void playerChoiceAfterMigrationPersistsAcrossLaterStarts() throws Exception {
        ConfigPatchContext context = context();
        writeConfig(context, "{\"map_atlas_generation\":true}");
        ImmediatelyFastMapAtlasConfigMigrationPatch.apply(context);
        writeConfig(context, "{\"map_atlas_generation\":true,\"chosen_by_player\":true}");

        ImmediatelyFastMapAtlasConfigMigrationPatch.apply(context);

        JsonObject persisted = readConfig(context);
        assertTrue(persisted.get("map_atlas_generation").getAsBoolean());
        assertTrue(persisted.get("chosen_by_player").getAsBoolean());
    }

    @Test
    void recordsMigrationWithoutChangingAnExistingDisabledChoice() throws Exception {
        ConfigPatchContext context = context();
        String disabledConfig = "{\"map_atlas_generation\":false,\"player_value\":23}";
        writeConfig(context, disabledConfig);

        ImmediatelyFastMapAtlasConfigMigrationPatch.apply(context);

        assertEquals(disabledConfig, Files.readString(configFile(context)));
        assertTrue(Files.exists(ImmediatelyFastMapAtlasConfigMigrationPatch.markerFile(
                context.persistentConfigDirectory())));
    }

    @Test
    void addsDisabledSettingWhenOldConfigDoesNotContainIt() throws Exception {
        ConfigPatchContext context = context();
        writeConfig(context, "{\"fast_text_lookup\":true}");

        ImmediatelyFastMapAtlasConfigMigrationPatch.apply(context);

        JsonObject migrated = readConfig(context);
        assertFalse(migrated.get("map_atlas_generation").getAsBoolean());
        assertTrue(migrated.get("fast_text_lookup").getAsBoolean());
    }

    private ConfigPatchContext context() {
        return new ConfigPatchContext(
                temporaryDirectory,
                temporaryDirectory.resolve("config"),
                temporaryDirectory.resolve("ec-config"),
                Set.of("immediatelyfast"));
    }

    private void writeConfig(ConfigPatchContext context, String content) throws Exception {
        Files.createDirectories(context.persistentConfigDirectory());
        Files.writeString(configFile(context), content);
    }

    private JsonObject readConfig(ConfigPatchContext context) throws Exception {
        return JsonParser.parseString(Files.readString(configFile(context))).getAsJsonObject();
    }

    private Path configFile(ConfigPatchContext context) {
        return context.persistentConfigDirectory().resolve(
                ImmediatelyFastMapAtlasConfigMigrationPatch.CONFIG_FILE_NAME);
    }
}
