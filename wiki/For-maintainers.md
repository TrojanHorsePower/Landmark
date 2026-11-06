# For maintainers

The full, always-current notes are in the repository README ("Building" and "For a new maintainer"). In short:

## Building

Requires Java 25 (the build tool needs it; the jars for 1.21.x still target Java 21).

```
./gradlew build                    # the default era (26.3): jar in build/libs, runs the JUnit tests
./gradlew build -Pera=1.21.9       # another Minecraft version range; see eras/
node --test tools/export.test.js
```

Each file in `eras/` is one jar: the Minecraft version it is compiled against, the range it supports, the dependency
versions and which per-version source folders it uses. `./gradlew runClient -Pera=<era>` starts a dev client for it.

## Several Minecraft versions

Everything that differs between versions sits behind `landmark.compat`: `Gfx` (drawing), `LandmarkScreen` /
`LandmarkListEntry` (screens, input and list rows), `LandmarkTexture`, `Pixels` and `Platform`. The common code in
`landmark.client` never names a class or method that changed between versions. Each era lists its implementations in
`era_sources` (folders under `src/era/`).

## Adding a Minecraft version

If an existing era's range can include it, widen `minecraft_range` and run the dev client on it
(`-Pminecraft_version=...`). Otherwise copy the closest `eras/*.properties`, point `minecraft_version`,
`fabric_api_version`, `modmenu_version` and `journeymap_api_version` at the new version, set `loader_min` to the higher of what
Fabric API and Mod Menu require, and fix whatever the compiler reports by adding or changing a folder under `src/era/`. The CI
and release workflows pick the new file up by themselves.

## Releases

`release.yml` runs when you push a tag `vX.Y.Z` that matches `mod_version` in `gradle.properties`, builds every era and attaches
all the jars to a GitHub release. It uses only the built-in `GITHUB_TOKEN`.

After the release, `curseforge.yml` uploads the same jars to CurseForge with `tools/curseforge-upload.js` (each jar is tagged
with the Minecraft versions it covers, Fabric, Client and its Java version). It needs a `CURSEFORGE_TOKEN` repository secret
(an API token from your CurseForge account) and a `CURSEFORGE_PROJECT_ID` repository variable (the project's numeric ID, shown
on its CurseForge page), and does nothing without them. Run it by hand from the Actions tab to retry or to upload an older tag.
`node tools/curseforge-upload.js <tag> <jar folder> --dry-run` prints what would be uploaded. To support a new Minecraft
version range, add its label to `JARS` in that script (a test fails until you do).

## This wiki and the website

Both live in the repository so changes are reviewed with the code: the wiki pages are in `wiki/` and the website in `site/`.
Edit them there; workflows publish them. Edits made directly in the GitHub wiki editor are overwritten by the next sync.
