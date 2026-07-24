package net.easecation.ecconfigredirect.patch;

import net.neoforged.fml.loading.FMLPaths;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NeoForgeConfigDirectoryPatch {

    private static final String CONTROLLED_BEDROCK_DIRECTORY = "bedrock-loader";

    private NeoForgeConfigDirectoryPatch() {
    }

    public static void apply(ConfigPatchContext context) throws Exception {
        Path directory = context.persistentConfigDirectory();
        Files.createDirectories(directory);

        Field absolutePath = FMLPaths.class.getDeclaredField("absolutePath");
        if (!absolutePath.trySetAccessible()) {
            throw new IllegalAccessException("FMLPaths.absolutePath is not accessible");
        }
        absolutePath.set(FMLPaths.CONFIGDIR, directory);

        if (!directory.equals(FMLPaths.CONFIGDIR.get())) {
            throw new IllegalStateException("NeoForge did not retain the redirected config directory");
        }
        ConfigPatchContext.LOGGER.info(
                "Player mod configuration redirected to {}; BedrockLoader remains at {}",
                directory,
                context.legacyConfigDirectory().resolve(CONTROLLED_BEDROCK_DIRECTORY));
    }
}

