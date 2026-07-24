package net.easecation.ecconfigredirect.patch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResourcePackDirectoryPatch {

    public static final String LEGACY_DIRECTORY_NAME = "resourcepacks";
    public static final String PERSISTENT_DIRECTORY_NAME = "ec-resourcepacks";

    private ResourcePackDirectoryPatch() {
    }

    public static void apply(ConfigPatchContext context) throws IOException {
        Path legacyDirectory = context.gameDirectory().resolve(LEGACY_DIRECTORY_NAME);
        Path persistentDirectory = context.gameDirectory().resolve(PERSISTENT_DIRECTORY_NAME);
        Files.createDirectories(persistentDirectory);
        int migrated = PersistentDirectoryMigration.copyMissingFiles(
                legacyDirectory,
                persistentDirectory,
                "resource pack");
        ConfigPatchContext.LOGGER.info(
                "Prepared persistent resource-pack directory {}; migrated {} missing file(s)",
                persistentDirectory,
                migrated);
    }

    public static Path resolveDirectory(Path gameDirectory, Path requestedDirectory) throws IOException {
        Path normalizedGameDirectory = gameDirectory.toAbsolutePath().normalize();
        Path normalizedRequestedDirectory = requestedDirectory.toAbsolutePath().normalize();
        Path legacyDirectory = normalizedGameDirectory.resolve(LEGACY_DIRECTORY_NAME);
        if (!normalizedRequestedDirectory.equals(legacyDirectory)) {
            return normalizedRequestedDirectory;
        }

        Path persistentDirectory = normalizedGameDirectory.resolve(PERSISTENT_DIRECTORY_NAME);
        Files.createDirectories(persistentDirectory);
        return persistentDirectory;
    }

    static int migrateMissingFiles(Path source, Path target) throws IOException {
        return PersistentDirectoryMigration.copyMissingFiles(source, target, "resource pack");
    }
}
