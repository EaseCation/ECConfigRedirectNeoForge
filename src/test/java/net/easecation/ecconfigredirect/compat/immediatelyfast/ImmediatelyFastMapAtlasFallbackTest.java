package net.easecation.ecconfigredirect.compat.immediatelyfast;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmediatelyFastMapAtlasFallbackTest {

    @BeforeEach
    void resetState() {
        ImmediatelyFastMapAtlasFallback.resetForTests();
    }

    @Test
    void normalMapAtlasAllocationKeepsOptimizedPathActive() {
        Object atlas = new Object();
        AtomicBoolean fallbackCalled = new AtomicBoolean();

        Object result = ImmediatelyFastMapAtlasFallback.allocateOrFallback(
                4096,
                4096,
                4,
                () -> atlas,
                () -> {
                    fallbackCalled.set(true);
                    return new Object();
                });

        assertSame(atlas, result);
        assertFalse(fallbackCalled.get());
        assertFalse(ImmediatelyFastMapAtlasFallback.isFallbackActive());
    }

    @Test
    void allocationFailureIsContainedBeforeItCanReachPacketHandling() {
        Object vanillaSentinel = new Object();

        Object result = ImmediatelyFastMapAtlasFallback.allocateOrFallback(
                4096,
                4096,
                4,
                () -> {
                    throw new IllegalStateException(
                            "Unable to allocate texture of size 4096x4096 (4 channels)");
                },
                () -> vanillaSentinel);

        assertSame(vanillaSentinel, result);
        assertTrue(ImmediatelyFastMapAtlasFallback.isFallbackActive());
    }

    @Test
    void activeFallbackDoesNotRetryTheLargeAllocation() {
        AtomicInteger atlasAttempts = new AtomicInteger();
        Object vanillaSentinel = new Object();
        ImmediatelyFastMapAtlasFallback.allocateOrFallback(
                4096,
                4096,
                4,
                () -> {
                    atlasAttempts.incrementAndGet();
                    throw new IllegalStateException(
                            "Unable to allocate texture of size 4096x4096 (4 channels)");
                },
                Object::new);

        Object secondResult = ImmediatelyFastMapAtlasFallback.allocateOrFallback(
                4096,
                4096,
                4,
                () -> {
                    atlasAttempts.incrementAndGet();
                    return new Object();
                },
                () -> vanillaSentinel);

        assertEquals(1, atlasAttempts.get());
        assertSame(vanillaSentinel, secondResult);
    }

    @Test
    void unrelatedTextureFailureIsNotHidden() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> ImmediatelyFastMapAtlasFallback.allocateOrFallback(
                        4096,
                        4096,
                        4,
                        () -> {
                            throw new IllegalStateException("GPU device was lost");
                        },
                        Object::new));

        assertEquals("GPU device was lost", failure.getMessage());
        assertFalse(ImmediatelyFastMapAtlasFallback.isFallbackActive());
    }

    @Test
    void ordinaryNonAtlasTexturesAreNeverIntercepted() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> ImmediatelyFastMapAtlasFallback.allocateOrFallback(
                        128,
                        128,
                        4,
                        () -> {
                            throw new IllegalStateException(
                                    "Unable to allocate texture of size 128x128 (4 channels)");
                        },
                        Object::new));

        assertEquals(
                "Unable to allocate texture of size 128x128 (4 channels)",
                failure.getMessage());
        assertFalse(ImmediatelyFastMapAtlasFallback.isFallbackActive());
    }

    @Test
    void resourceReloadedMappingsStayOnVanillaPathAfterFailure() {
        ImmediatelyFastMapAtlasFallback.allocateOrFallback(
                4096,
                4096,
                4,
                () -> {
                    throw new IllegalStateException(
                            "Unable to allocate texture of size 4096x4096 (4 channels)");
                },
                Object::new);
        FakeMapTextureManager manager = new FakeMapTextureManager();

        assertTrue(ImmediatelyFastMapAtlasFallback.forceVanillaMapping(manager, 7));
        assertEquals(-1, manager.immediatelyFast$mapIdToAtlasMapping.get(7));

        manager.immediatelyFast$mapIdToAtlasMapping.clear();
        assertTrue(ImmediatelyFastMapAtlasFallback.forceVanillaMapping(manager, 8));
        assertEquals(-1, manager.immediatelyFast$mapIdToAtlasMapping.get(8));
    }

    private static final class FakeMapTextureManager {
        private final Int2IntMap immediatelyFast$mapIdToAtlasMapping = new Int2IntOpenHashMap();
    }
}
