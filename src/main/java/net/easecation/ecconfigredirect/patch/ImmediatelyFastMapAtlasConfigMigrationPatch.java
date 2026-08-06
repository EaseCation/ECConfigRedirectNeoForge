package net.easecation.ecconfigredirect.patch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * One-time migration for the unsafe 4096x4096 ImmediatelyFast map atlas default.
 *
 * <p>The marker is deliberately separate from {@code immediatelyfast.json}: ImmediatelyFast
 * rewrites that file from its typed config model and would discard an unknown migration field.
 */
public final class ImmediatelyFastMapAtlasConfigMigrationPatch {

    static final String CONFIG_FILE_NAME = "immediatelyfast.json";
    static final String SETTING_NAME = "map_atlas_generation";
    static final String MIGRATION_ID = "immediatelyfast-map-atlas-generation-v1";
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private ImmediatelyFastMapAtlasConfigMigrationPatch() {
    }

    public static void apply(ConfigPatchContext context) throws IOException {
        Path configFile = context.persistentConfigDirectory().resolve(CONFIG_FILE_NAME);
        Path markerFile = markerFile(context.persistentConfigDirectory());
        if (Files.exists(markerFile)) {
            return;
        }
        if (!Files.isRegularFile(configFile)) {
            throw new IOException("ImmediatelyFast config is unavailable for migration: " + configFile);
        }

        JsonObject config = readConfig(configFile);
        JsonElement setting = config.get(SETTING_NAME);
        boolean changed = setting == null || requireBoolean(setting, configFile);
        if (changed) {
            config.addProperty(SETTING_NAME, false);
            writeConfigAtomically(configFile, config);
        }

        writeMarker(markerFile);
        ConfigPatchContext.LOGGER.info(
                "Applied config migration {} (map atlas changed={})", MIGRATION_ID, changed);
    }

    static Path markerFile(Path persistentConfigDirectory) {
        return persistentConfigDirectory
                .resolve(".ecconfigredirect")
                .resolve("migrations")
                .resolve(MIGRATION_ID + ".applied");
    }

    private static JsonObject readConfig(Path configFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                throw new IOException("ImmediatelyFast config root is not an object: " + configFile);
            }
            return root.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException exception) {
            throw new IOException("Could not parse ImmediatelyFast config: " + configFile, exception);
        }
    }

    private static boolean requireBoolean(JsonElement setting, Path configFile) throws IOException {
        if (!setting.isJsonPrimitive() || !setting.getAsJsonPrimitive().isBoolean()) {
            throw new IOException(
                    "ImmediatelyFast " + SETTING_NAME + " is not a boolean: " + configFile);
        }
        return setting.getAsBoolean();
    }

    private static void writeConfigAtomically(Path configFile, JsonObject config) throws IOException {
        Path parent = configFile.getParent();
        Path temporaryFile = Files.createTempFile(parent, CONFIG_FILE_NAME + ".", ".tmp");
        boolean moved = false;
        try {
            try (Writer writer = Files.newBufferedWriter(
                    temporaryFile,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                PRETTY_GSON.toJson(config, writer);
                writer.write(System.lineSeparator());
            }
            try {
                Files.move(
                        temporaryFile,
                        configFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    private static void writeMarker(Path markerFile) throws IOException {
        Files.createDirectories(markerFile.getParent());
        try {
            Files.writeString(
                    markerFile,
                    "migration=" + MIGRATION_ID + System.lineSeparator(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException ignored) {
            // Another startup path completed the same idempotent migration.
        }
    }
}
