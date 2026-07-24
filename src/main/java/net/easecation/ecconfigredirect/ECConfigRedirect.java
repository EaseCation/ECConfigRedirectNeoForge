package net.easecation.ecconfigredirect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** NeoForge entry point; the redirect itself runs earlier through the Mixin plugin. */
@Mod(value = ECConfigRedirect.MOD_ID, dist = Dist.CLIENT)
public final class ECConfigRedirect {

    public static final String MOD_ID = "ecconfigredirect";
}

