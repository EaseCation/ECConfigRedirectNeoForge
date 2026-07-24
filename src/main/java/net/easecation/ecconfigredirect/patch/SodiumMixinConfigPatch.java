package net.easecation.ecconfigredirect.patch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SodiumMixinConfigPatch {

    private static final String FILE_NAME = "sodium-mixins.properties";

    private SodiumMixinConfigPatch() {
    }

    public static void apply(ConfigPatchContext context) throws IOException {
        bridgeHardcodedPath(context.legacyConfigDirectory(), context.persistentConfigDirectory());
    }

    static void bridgeHardcodedPath(Path legacyDirectory, Path persistentDirectory) throws IOException {
        Path legacyFile = legacyDirectory.resolve(FILE_NAME);
        Path persistentFile = persistentDirectory.resolve(FILE_NAME);
        Files.createDirectories(legacyDirectory);
        Files.createDirectories(persistentDirectory);

        if (Files.notExists(persistentFile, LinkOption.NOFOLLOW_LINKS)) {
            Files.createFile(persistentFile);
        }
        if (Files.exists(legacyFile, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(legacyFile) && Files.isSameFile(legacyFile, persistentFile)) {
                return;
            }
            if (Files.isDirectory(legacyFile, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Sodium mixin config path is a directory: " + legacyFile);
            }
            Files.delete(legacyFile);
        }

        try {
            Files.createLink(legacyFile, persistentFile);
        } catch (IOException | UnsupportedOperationException exception) {
            try {
                Files.copy(persistentFile, legacyFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException restoreException) {
                exception.addSuppressed(restoreException);
            }
            throw new IOException("Could not bridge Sodium's hard-coded mixin config path", exception);
        }
        ConfigPatchContext.LOGGER.info(
                "Sodium mixin configuration linked from {} to {}", legacyFile, persistentFile);
    }
}
