package net.easecation.ecconfigredirect.patch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ShaderPackDirectoryPatch {

    public static final String LEGACY_DIRECTORY_NAME = "shaderpacks";
    public static final String PERSISTENT_DIRECTORY_NAME = "ec-shaderpacks";

    private ShaderPackDirectoryPatch() {
    }

    public static void apply(ConfigPatchContext context) throws IOException {
        Path legacyDirectory = context.gameDirectory().resolve(LEGACY_DIRECTORY_NAME);
        Path persistentDirectory = persistentDirectory(context.gameDirectory());
        int migrated = PersistentDirectoryMigration.copyMissingFiles(
                legacyDirectory,
                persistentDirectory,
                "Iris shader pack");
        ConfigPatchContext.LOGGER.info(
                "Prepared persistent Iris shader-pack directory {}; migrated {} missing file(s)",
                persistentDirectory,
                migrated);
    }

    public static Path persistentDirectory(Path gameDirectory) throws IOException {
        Path directory = gameDirectory.toAbsolutePath().normalize().resolve(PERSISTENT_DIRECTORY_NAME);
        Files.createDirectories(directory);
        return directory;
    }
}
