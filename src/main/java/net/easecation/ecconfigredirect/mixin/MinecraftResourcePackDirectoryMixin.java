package net.easecation.ecconfigredirect.mixin;

import net.easecation.ecconfigredirect.patch.ConfigPatchContext;
import net.easecation.ecconfigredirect.patch.ResourcePackDirectoryPatch;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Mixin(Minecraft.class)
public abstract class MinecraftResourcePackDirectoryMixin {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;toPath()Ljava/nio/file/Path;",
                    ordinal = 0))
    private Path ecconfigredirect$redirectResourcePackDirectory(File requestedDirectory) {
        Path original = requestedDirectory.toPath();
        try {
            Path redirected = ResourcePackDirectoryPatch.resolveDirectory(FMLPaths.GAMEDIR.get(), original);
            if (!redirected.equals(original.toAbsolutePath().normalize())) {
                ConfigPatchContext.LOGGER.info(
                        "Player resource packs redirected from {} to {}", original, redirected);
            }
            return redirected;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not prepare the persistent resource-pack directory", exception);
        }
    }
}
