# EC Config Redirect

Client bootstrap mod for the EaseCation NeoForge distribution. It redirects the
shared NeoForge mod configuration directory from `<gameDir>/config` to
`<gameDir>/ec-config` before Iris, Sodium, and other early-loading client mods read
their configuration. Minecraft's default `<gameDir>/resourcepacks` directory is
redirected to `<gameDir>/ec-resourcepacks`, so manually installed Java resource packs
survive launcher cleanup. Iris shader packs are similarly redirected from
`<gameDir>/shaderpacks` to `<gameDir>/ec-shaderpacks` when Iris is installed.

The launcher-controlled files stay in their original locations:

- `<gameDir>/config/fml.toml`
- the complete `<gameDir>/config/bedrock-loader` tree, including `plugins`

On first use, files missing from `ec-config` are copied from the legacy `config`
directory. Existing persistent files are never overwritten.

Resource packs still present in the legacy `resourcepacks` directory are migrated to
`ec-resourcepacks` without replacing persistent files. A resource-pack directory
explicitly supplied by a launcher is respected and is not redirected. BedrockLoader's
generated and remote packs remain under its own controlled directories.

Shader packs still present in the legacy `shaderpacks` directory are copied to
`ec-shaderpacks` without overwriting persistent files. The migration patch declares
`iris` as a required Mod and is skipped when Iris is absent. Its `@Pseudo` Mixin does
not link against Iris classes, so an installation without Iris remains valid.

Sodium 0.7.x hard-codes `./config/sodium-mixins.properties` instead of using the
NeoForge config path. The bootstrap recreates that legacy path as a hard link to
`ec-config/sodium-mixins.properties`, so its canonical data survives launcher cleanup.

## Patch registry

Early startup behavior is declared in `ConfigPatchRegistry.PATCHES`. Each patch has
an ID, a required-Mod set, and a failure policy. A patch for an absent optional Mod
is skipped without loading any of that Mod's classes. Compatibility and default-file
failures are isolated and logged; only failure of the core NeoForge directory redirect
stops startup, because silently falling back to the launcher's disposable directory
would lose player settings.

Bundled defaults are listed in `BundledDefaultConfigsPatch.DEFAULT_FILES`. A default
is released only when its owning Mod is installed and its destination does not exist.
Legacy migrated files and player-edited persistent files therefore always win. The
current bundle covers Sodium, Iris, ImmediatelyFast, IMBlocker, YACL,
ViaBedrockUtility, ECClientDiagnostics, and ECClientSettings. The diagnostics
default uses the production endpoint so the NetEase archive can omit `ec-config`;
the bootstrap releases it on first startup. A pre-rendered Packwiz channel config
still wins because bundled defaults never overwrite an existing file.

`ResourcePackDirectoryPatch` prepares and migrates `ec-resourcepacks` in the registry.
Migration failures are isolated and logged like other compatibility work; the client-side
redirect independently verifies that the persistent directory itself is usable.
`MinecraftResourcePackDirectoryMixin` then replaces only Minecraft's default resource-pack
directory while the client constructs its repository. Keeping resolution separate from
migration makes the filesystem behavior unit-testable and keeps the version-sensitive
Minecraft injection small.
