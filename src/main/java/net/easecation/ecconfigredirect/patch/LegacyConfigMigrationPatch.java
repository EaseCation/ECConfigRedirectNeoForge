package net.easecation.ecconfigredirect.patch;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class LegacyConfigMigrationPatch {

    private static final String CONTROLLED_BEDROCK_DIRECTORY = "bedrock-loader";
    private static final String EARLY_FML_CONFIG = "fml.toml";

    private LegacyConfigMigrationPatch() {
    }

    public static void apply(ConfigPatchContext context) throws IOException {
        Files.createDirectories(context.persistentConfigDirectory());
        int migrated = migrateMissingFiles(
                context.legacyConfigDirectory(),
                context.persistentConfigDirectory());
        ConfigPatchContext.LOGGER.info("Migrated {} missing legacy config file(s) to {}",
                migrated, context.persistentConfigDirectory());
    }

    static int migrateMissingFiles(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return 0;
        }

        int[] migratedFiles = {0};
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                if (!directory.equals(source)) {
                    Path relative = source.relativize(directory);
                    if (relative.getNameCount() == 1
                            && CONTROLLED_BEDROCK_DIRECTORY.equals(relative.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    Files.createDirectories(target.resolve(relative));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Path relative = source.relativize(file);
                if (relative.getNameCount() == 1
                        && EARLY_FML_CONFIG.equals(relative.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                if (attributes.isSymbolicLink()) {
                    ConfigPatchContext.LOGGER.warn("Skipping symbolic link while migrating legacy config: {}", file);
                    return FileVisitResult.CONTINUE;
                }

                Path destination = target.resolve(relative);
                if (Files.notExists(destination)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(file, destination);
                    migratedFiles[0]++;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return migratedFiles[0];
    }
}

