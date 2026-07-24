package net.easecation.ecconfigredirect.patch;

import java.util.List;
import java.util.Set;

import static net.easecation.ecconfigredirect.patch.ConfigPatchDefinition.FailurePolicy.FATAL;
import static net.easecation.ecconfigredirect.patch.ConfigPatchDefinition.FailurePolicy.WARN_AND_CONTINUE;

public final class ConfigPatchRegistry {

    /** Ordered startup patches. Add new compatibility workarounds here. */
    public static final List<ConfigPatchDefinition> PATCHES = List.of(
            new ConfigPatchDefinition(
                    "migrate-legacy-configs",
                    Set.of(),
                    WARN_AND_CONTINUE,
                    LegacyConfigMigrationPatch::apply),
            new ConfigPatchDefinition(
                    "install-bundled-defaults",
                    Set.of(),
                    WARN_AND_CONTINUE,
                    BundledDefaultConfigsPatch::apply),
            new ConfigPatchDefinition(
                    "bridge-sodium-mixin-config",
                    Set.of("sodium"),
                    WARN_AND_CONTINUE,
                    SodiumMixinConfigPatch::apply),
            new ConfigPatchDefinition(
                    "migrate-legacy-resource-packs",
                    Set.of(),
                    WARN_AND_CONTINUE,
                    ResourcePackDirectoryPatch::apply),
            new ConfigPatchDefinition(
                    "migrate-legacy-iris-shader-packs",
                    Set.of("iris"),
                    WARN_AND_CONTINUE,
                    ShaderPackDirectoryPatch::apply),
            new ConfigPatchDefinition(
                    "redirect-neoforge-config-directory",
                    Set.of(),
                    FATAL,
                    NeoForgeConfigDirectoryPatch::apply));

    private ConfigPatchRegistry() {
    }

    public static void applyAll(ConfigPatchContext context) {
        applyAll(context, PATCHES);
    }

    static void applyAll(ConfigPatchContext context, List<ConfigPatchDefinition> patches) {
        for (ConfigPatchDefinition definition : patches) {
            if (!definition.appliesTo(context)) {
                ConfigPatchContext.LOGGER.debug(
                        "Skipping config patch {} because required mod(s) are absent: {}",
                        definition.id(),
                        definition.requiredModIds());
                continue;
            }

            try {
                definition.patch().apply(context);
                ConfigPatchContext.LOGGER.debug("Applied config patch {}", definition.id());
            } catch (Exception | LinkageError exception) {
                if (definition.failurePolicy() == FATAL) {
                    throw new IllegalStateException("Required config patch failed: " + definition.id(), exception);
                }
                ConfigPatchContext.LOGGER.warn(
                        "Optional config patch {} failed; startup will continue",
                        definition.id(),
                        exception);
            }
        }
    }
}
