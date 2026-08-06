package net.easecation.ecconfigredirect.mixin;

import net.easecation.ecconfigredirect.compat.immediatelyfast.ImmediatelyFastMapAtlasFallback;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Contains the known 4096x4096 allocation failure inside ImmediatelyFast's atlas constructor. */
@Pseudo
@Mixin(targets = "net.raphimc.immediatelyfast.feature.map_atlas_generation.MapAtlasTexture", remap = false)
public abstract class ImmediatelyFastMapAtlasTextureMixin {

    @Unique
    private boolean ecconfigredirect$allocationFailed;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/String;IIZ)Lnet/minecraft/client/renderer/texture/DynamicTexture;",
                    remap = false),
            remap = false)
    private DynamicTexture ecconfigredirect$allocateAtlasOrFallback(
            String label,
            int width,
            int height,
            boolean useCalloc) {
        DynamicTexture texture = ImmediatelyFastMapAtlasFallback.createAtlasTexture(
                label, width, height, useCalloc);
        this.ecconfigredirect$allocationFailed = ImmediatelyFastMapAtlasFallback.isFallbackActive();
        return texture;
    }

    @Inject(method = "getNextMapLocation", at = @At("HEAD"), cancellable = true, remap = false)
    private void ecconfigredirect$useVanillaTextureAfterAllocationFailure(
            CallbackInfoReturnable<Integer> callback) {
        if (this.ecconfigredirect$allocationFailed) {
            callback.setReturnValue(-1);
        }
    }
}
