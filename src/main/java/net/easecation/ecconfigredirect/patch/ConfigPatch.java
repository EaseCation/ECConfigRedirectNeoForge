package net.easecation.ecconfigredirect.patch;

@FunctionalInterface
public interface ConfigPatch {

    void apply(ConfigPatchContext context) throws Exception;
}

