package net.easecation.ecconfigredirect.patch;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

final class PersistentDirectoryMigration {

    private PersistentDirectoryMigration() {
    }

    static int copyMissingFiles(Path source, Path target, String contentDescription) throws IOException {
        if (!Files.isDirectory(source)) {
            return 0;
        }

        int[] migratedFiles = {0};
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                if (!directory.equals(source)) {
                    Files.createDirectories(target.resolve(source.relativize(directory)));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (attributes.isSymbolicLink()) {
                    ConfigPatchContext.LOGGER.warn(
                            "Skipping symbolic link while migrating legacy {}: {}",
                            contentDescription,
                            file);
                    return FileVisitResult.CONTINUE;
                }

                Path destination = target.resolve(source.relativize(file));
                Files.createDirectories(destination.getParent());
                try {
                    Files.copy(file, destination);
                    migratedFiles[0]++;
                } catch (FileAlreadyExistsException ignored) {
                    // Persistent player data always wins over the disposable legacy directory.
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return migratedFiles[0];
    }
}
