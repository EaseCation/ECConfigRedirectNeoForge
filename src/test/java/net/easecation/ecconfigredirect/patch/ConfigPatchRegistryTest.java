package net.easecation.ecconfigredirect.patch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static net.easecation.ecconfigredirect.patch.ConfigPatchDefinition.FailurePolicy.FATAL;
import static net.easecation.ecconfigredirect.patch.ConfigPatchDefinition.FailurePolicy.WARN_AND_CONTINUE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPatchRegistryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void declaresStartupPatchesInDependencyOrder() {
        assertEquals(
                List.of(
                        "migrate-legacy-configs",
                        "install-bundled-defaults",
                        "bridge-sodium-mixin-config",
                        "migrate-legacy-resource-packs",
                        "migrate-legacy-iris-shader-packs",
                        "redirect-neoforge-config-directory"),
                ConfigPatchRegistry.PATCHES.stream().map(ConfigPatchDefinition::id).toList());
    }

    @Test
    void skipsPatchCleanlyWhenItsOptionalModIsAbsent() {
        AtomicBoolean invoked = new AtomicBoolean();
        ConfigPatchDefinition sodiumPatch = new ConfigPatchDefinition(
                "sodium-test", Set.of("sodium"), WARN_AND_CONTINUE, context -> invoked.set(true));

        assertDoesNotThrow(() -> ConfigPatchRegistry.applyAll(context(Set.of()), List.of(sodiumPatch)));
        assertFalse(invoked.get());
    }

    @Test
    void isolatesOptionalPatchFailuresButRejectsCoreFailures() {
        ConfigPatchDefinition optionalFailure = new ConfigPatchDefinition(
                "optional-test", Set.of(), WARN_AND_CONTINUE, context -> {
                    throw new NoClassDefFoundError("optional Mod API is absent");
                });
        ConfigPatchDefinition fatalFailure = new ConfigPatchDefinition(
                "fatal-test", Set.of(), FATAL, context -> {
                    throw new IllegalStateException("fatal failure");
                });

        assertDoesNotThrow(() -> ConfigPatchRegistry.applyAll(context(Set.of()), List.of(optionalFailure)));
        assertThrows(
                IllegalStateException.class,
                () -> ConfigPatchRegistry.applyAll(context(Set.of()), List.of(fatalFailure)));
    }

    @Test
    void installsDefaultsOnlyForPresentModsAndNeverOverwritesPlayers() throws Exception {
        ConfigPatchContext context = context(Set.of("sodium", "iris"));
        Files.createDirectories(context.persistentConfigDirectory());
        Path sodiumOptions = context.persistentConfigDirectory().resolve("sodium-options.json");
        Files.writeString(sodiumOptions, "player-value");

        BundledDefaultConfigsPatch.apply(context);

        assertEquals("player-value", Files.readString(sodiumOptions));
        assertTrue(Files.exists(context.persistentConfigDirectory().resolve("sodium-mixins.properties")));
        assertTrue(Files.exists(context.persistentConfigDirectory().resolve("iris.properties")));
        assertFalse(Files.exists(context.persistentConfigDirectory().resolve("immediatelyfast.json")));
        assertFalse(Files.exists(context.persistentConfigDirectory().resolve("viabedrockutility.json")));
    }

    @Test
    void everyDeclaredBundledDefaultCanBeInstalled() throws Exception {
        Set<String> allDefaultOwners = BundledDefaultConfigsPatch.DEFAULT_FILES.stream()
                .map(BundledDefaultConfigsPatch.DefaultConfigFile::modId)
                .collect(Collectors.toSet());
        ConfigPatchContext context = context(allDefaultOwners);

        BundledDefaultConfigsPatch.apply(context);

        for (BundledDefaultConfigsPatch.DefaultConfigFile defaultFile
                : BundledDefaultConfigsPatch.DEFAULT_FILES) {
            assertTrue(
                    Files.exists(context.persistentConfigDirectory().resolve(defaultFile.relativePath())),
                    () -> "Bundled default was not installed: " + defaultFile);
        }
    }

    private ConfigPatchContext context(Set<String> installedMods) {
        return new ConfigPatchContext(
                temporaryDirectory,
                temporaryDirectory.resolve("config"),
                temporaryDirectory.resolve("ec-config"),
                installedMods);
    }
}
