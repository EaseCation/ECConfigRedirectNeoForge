package net.easecation.ecconfigredirect.patch;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public record ConfigPatchContext(
        Path gameDirectory,
        Path legacyConfigDirectory,
        Path persistentConfigDirectory,
        Set<String> installedModIds) {

    public static final String PERSISTENT_DIRECTORY_NAME = "ec-config";
    public static final Logger LOGGER = LoggerFactory.getLogger("ECConfigRedirect");

    public ConfigPatchContext {
        gameDirectory = gameDirectory.toAbsolutePath().normalize();
        legacyConfigDirectory = legacyConfigDirectory.toAbsolutePath().normalize();
        persistentConfigDirectory = persistentConfigDirectory.toAbsolutePath().normalize();
        installedModIds = Set.copyOf(installedModIds);
    }

    public static ConfigPatchContext forCurrentGame() {
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Set<String> installedMods = new LinkedHashSet<>();
        if (FMLLoader.getLoadingModList() != null) {
            FMLLoader.getLoadingModList().getMods().forEach(mod -> installedMods.add(mod.getModId()));
        } else {
            LOGGER.warn("NeoForge loading mod list is unavailable; optional config patches will be skipped");
        }
        return new ConfigPatchContext(
                gameDirectory,
                gameDirectory.resolve("config"),
                gameDirectory.resolve(PERSISTENT_DIRECTORY_NAME),
                installedMods);
    }

    public boolean isModLoaded(String modId) {
        return installedModIds.contains(modId);
    }
}

