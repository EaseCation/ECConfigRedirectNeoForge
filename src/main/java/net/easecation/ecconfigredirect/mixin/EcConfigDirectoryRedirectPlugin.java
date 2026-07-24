package net.easecation.ecconfigredirect.mixin;

import net.easecation.ecconfigredirect.patch.ConfigPatchContext;
import net.easecation.ecconfigredirect.patch.ConfigPatchRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/** Runs the configuration patch registry before early client mods initialize. */
public final class EcConfigDirectoryRedirectPlugin implements IMixinConfigPlugin {

    private static boolean installed;

    @Override
    public synchronized void onLoad(String mixinPackage) {
        if (!installed && isClientEnvironment()) {
            ConfigPatchRegistry.applyAll(ConfigPatchContext.forCurrentGame());
            installed = true;
        }
    }

    private static boolean isClientEnvironment() {
        try {
            try {
                Method getDist = FMLEnvironment.class.getMethod("getDist");
                return getDist.invoke(null) == Dist.CLIENT;
            } catch (NoSuchMethodException ignored) {
                Field dist = FMLEnvironment.class.getField("dist");
                return dist.get(null) == Dist.CLIENT;
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not determine the NeoForge distribution", exception);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

