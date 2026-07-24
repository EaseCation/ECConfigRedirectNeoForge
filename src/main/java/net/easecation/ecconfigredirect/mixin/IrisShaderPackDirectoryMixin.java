package net.easecation.ecconfigredirect.mixin;

import net.easecation.ecconfigredirect.patch.ConfigPatchContext;
import net.easecation.ecconfigredirect.patch.ShaderPackDirectoryPatch;
import net.neoforged.fml.loading.FMLPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Path;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public abstract class IrisShaderPackDirectoryMixin {

    @Unique
    private static boolean ecconfigredirect$loggedShaderPackRedirect;

    @Inject(
            method = "getShaderpacksDirectory",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0,
            expect = 1)
    private static void ecconfigredirect$redirectShaderPackDirectory(
            CallbackInfoReturnable<Path> callback) {
        try {
            Path redirected = ShaderPackDirectoryPatch.persistentDirectory(FMLPaths.GAMEDIR.get());
            if (!ecconfigredirect$loggedShaderPackRedirect) {
                ecconfigredirect$loggedShaderPackRedirect = true;
                ConfigPatchContext.LOGGER.info("Iris shader packs redirected to {}", redirected);
            }
            callback.setReturnValue(redirected);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not prepare the persistent Iris shader-pack directory", exception);
        }
    }
}
