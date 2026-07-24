package net.easecation.ecconfigredirect.patch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

public final class BundledDefaultConfigsPatch {

    /** Defaults are installed only when their owning Mod is present and the destination is absent. */
    public static final List<DefaultConfigFile> DEFAULT_FILES = List.of(
            file("sodium", "sodium-options.json"),
            file("sodium", "sodium-mixins.properties"),
            file("iris", "iris.properties"),
            file("iris", "iris-excluded.json"),
            file("immediatelyfast", "immediatelyfast.json"),
            file("imblocker", "imblocker.json"),
            file("yet_another_config_lib_v3", "yacl.json5"),
            file("viabedrockutility", "viabedrockutility.json"),
            file("ecclientsettings", "ecclientsettings-client.toml"),
            file("ecclientsettings", "ecclientsettings/profiles.json"),
            file("ecclientsettings", "ecclientsettings/profiles/default.json"));

    private BundledDefaultConfigsPatch() {
    }

    public static void apply(ConfigPatchContext context) throws IOException {
        Files.createDirectories(context.persistentConfigDirectory());
        int installed = 0;
        IOException failures = null;

        for (DefaultConfigFile defaultFile : DEFAULT_FILES) {
            if (!context.isModLoaded(defaultFile.modId())) {
                continue;
            }
            try {
                if (installIfMissing(context.persistentConfigDirectory(), defaultFile)) {
                    installed++;
                }
            } catch (IOException exception) {
                if (failures == null) {
                    failures = new IOException("One or more bundled defaults could not be installed");
                }
                failures.addSuppressed(exception);
            }
        }

        ConfigPatchContext.LOGGER.info("Installed {} missing bundled default config file(s)", installed);
        if (failures != null) {
            throw failures;
        }
    }

    static boolean installIfMissing(Path persistentDirectory, DefaultConfigFile defaultFile)
            throws IOException {
        Path normalizedRoot = persistentDirectory.toAbsolutePath().normalize();
        Path destination = normalizedRoot.resolve(defaultFile.relativePath()).normalize();
        if (!destination.startsWith(normalizedRoot)) {
            throw new IOException("Bundled default escapes the persistent config directory: " + defaultFile);
        }
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        Files.createDirectories(destination.getParent());
        try (InputStream input = BundledDefaultConfigsPatch.class.getResourceAsStream(defaultFile.resourcePath())) {
            if (input == null) {
                throw new FileNotFoundException("Bundled default resource is missing: " + defaultFile.resourcePath());
            }
            try {
                Files.copy(input, destination);
                return true;
            } catch (FileAlreadyExistsException ignored) {
                return false;
            }
        }
    }

    private static DefaultConfigFile file(String modId, String relativePath) {
        return new DefaultConfigFile(
                modId,
                "/defaults/" + relativePath.replace('\\', '/'),
                Path.of(relativePath));
    }

    public record DefaultConfigFile(String modId, String resourcePath, Path relativePath) {
    }
}

