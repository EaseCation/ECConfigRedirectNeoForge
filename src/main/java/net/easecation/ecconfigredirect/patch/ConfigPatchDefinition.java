package net.easecation.ecconfigredirect.patch;

import java.util.Set;

public record ConfigPatchDefinition(
        String id,
        Set<String> requiredModIds,
        FailurePolicy failurePolicy,
        ConfigPatch patch) {

    public ConfigPatchDefinition {
        requiredModIds = Set.copyOf(requiredModIds);
    }

    public boolean appliesTo(ConfigPatchContext context) {
        return requiredModIds.stream().allMatch(context::isModLoaded);
    }

    public enum FailurePolicy {
        FATAL,
        WARN_AND_CONTINUE
    }
}

