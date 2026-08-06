package net.easecation.ecconfigredirect.compat.immediatelyfast;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.easecation.ecconfigredirect.patch.ConfigPatchContext;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Process-wide fail-safe for ImmediatelyFast's optional map atlas optimization. */
public final class ImmediatelyFastMapAtlasFallback {

    static final int ATLAS_SIZE = 4096;
    static final int ATLAS_CHANNELS = 4;
    static final int FALLBACK_TEXTURE_SIZE = 1;
    private static final String MAPPING_FIELD_SUFFIX = "mapIdToAtlasMapping";
    private static final AtomicBoolean FALLBACK_ACTIVE = new AtomicBoolean();
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final Map<Class<?>, Optional<Field>> MAPPING_FIELDS = new ConcurrentHashMap<>();

    private ImmediatelyFastMapAtlasFallback() {
    }

    public static DynamicTexture createAtlasTexture(
            String label,
            int width,
            int height,
            boolean useCalloc) {
        return allocateOrFallback(
                width,
                height,
                ATLAS_CHANNELS,
                () -> new DynamicTexture(label, width, height, useCalloc),
                () -> new DynamicTexture(
                        label + " (vanilla map fallback)",
                        FALLBACK_TEXTURE_SIZE,
                        FALLBACK_TEXTURE_SIZE,
                        true));
    }

    static <T> T allocateOrFallback(
            int width,
            int height,
            int channels,
            Supplier<T> atlasAllocation,
            Supplier<T> fallbackAllocation) {
        if (!isImmediatelyFastAtlas(width, height, channels)) {
            return atlasAllocation.get();
        }
        if (FALLBACK_ACTIVE.get()) {
            return fallbackAllocation.get();
        }

        try {
            return atlasAllocation.get();
        } catch (IllegalStateException exception) {
            if (!isNativeImageAllocationFailure(exception, width, height, channels)) {
                throw exception;
            }
            if (FALLBACK_ACTIVE.compareAndSet(false, true)) {
                ConfigPatchContext.LOGGER.error(
                        "ImmediatelyFast could not allocate its {}x{} map atlas; disabling map atlases "
                                + "for this client session and falling back to vanilla per-map textures",
                        width,
                        height,
                        exception);
            }
            return fallbackAllocation.get();
        }
    }

    public static boolean isFallbackActive() {
        return FALLBACK_ACTIVE.get();
    }

    /**
     * Pre-seeds ImmediatelyFast's injected map-id mapping with {@code -1}. Its own map instance
     * mixin treats that value as an explicit request to use Minecraft's vanilla 128x128 texture.
     */
    public static boolean forceVanillaMapping(Object mapTextureManager, int mapId) {
        if (!FALLBACK_ACTIVE.get()) {
            return false;
        }

        Optional<Field> optionalField = MAPPING_FIELDS.computeIfAbsent(
                mapTextureManager.getClass(), ImmediatelyFastMapAtlasFallback::findMappingField);
        if (optionalField.isEmpty()) {
            logLookupFailure(mapTextureManager.getClass(), null);
            return false;
        }

        try {
            Object value = optionalField.get().get(mapTextureManager);
            if (value instanceof Int2IntMap mapping) {
                mapping.put(mapId, -1);
                return true;
            }
            logLookupFailure(mapTextureManager.getClass(), null);
        } catch (IllegalAccessException exception) {
            logLookupFailure(mapTextureManager.getClass(), exception);
        }
        return false;
    }

    static boolean isNativeImageAllocationFailure(
            IllegalStateException exception,
            int width,
            int height,
            int channels) {
        return ("Unable to allocate texture of size " + width + "x" + height
                + " (" + channels + " channels)").equals(exception.getMessage());
    }

    private static boolean isImmediatelyFastAtlas(int width, int height, int channels) {
        return width == ATLAS_SIZE && height == ATLAS_SIZE && channels == ATLAS_CHANNELS;
    }

    private static Optional<Field> findMappingField(Class<?> targetClass) {
        for (Class<?> type = targetClass; type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Int2IntMap.class.isAssignableFrom(field.getType())
                        && field.getName().endsWith(MAPPING_FIELD_SUFFIX)
                        && field.trySetAccessible()) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }

    private static void logLookupFailure(Class<?> targetClass, Exception exception) {
        if (!LOOKUP_FAILURE_LOGGED.compareAndSet(false, true)) {
            return;
        }
        if (exception == null) {
            ConfigPatchContext.LOGGER.error(
                    "ImmediatelyFast map atlas fallback could not find its map mapping on {}; "
                            + "future maps will still avoid the large allocation but may create tiny placeholders",
                    targetClass.getName());
        } else {
            ConfigPatchContext.LOGGER.error(
                    "ImmediatelyFast map atlas fallback could not access its map mapping on {}; "
                            + "future maps will still avoid the large allocation but may create tiny placeholders",
                    targetClass.getName(),
                    exception);
        }
    }

    static void resetForTests() {
        FALLBACK_ACTIVE.set(false);
        LOOKUP_FAILURE_LOGGED.set(false);
        MAPPING_FIELDS.clear();
    }
}
