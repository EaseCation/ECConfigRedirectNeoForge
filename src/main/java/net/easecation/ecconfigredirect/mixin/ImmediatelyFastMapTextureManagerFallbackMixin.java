package net.easecation.ecconfigredirect.mixin;

import net.easecation.ecconfigredirect.compat.immediatelyfast.ImmediatelyFastMapAtlasFallback;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps later maps on the vanilla path after one atlas allocation has failed. */
// ImmediatelyFast uses the default priority 1000. Applying at 900 makes this HEAD callback run
// first, so its computeIfAbsent observes the pre-seeded -1 mapping and does not create placeholders.
@Mixin(value = MapTextureManager.class, priority = 900)
public abstract class ImmediatelyFastMapTextureManagerFallbackMixin {

    @Inject(method = "getOrCreateMapInstance", at = @At("HEAD"))
    private void ecconfigredirect$forceVanillaMapTexture(
            MapId mapId,
            MapItemSavedData data,
            CallbackInfoReturnable<Object> callback) {
        ImmediatelyFastMapAtlasFallback.forceVanillaMapping(this, mapId.id());
    }
}
