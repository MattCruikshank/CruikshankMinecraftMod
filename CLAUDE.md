# Cruikshank Minecraft Mod

## Overview

Minecraft Forge mod for 1.20.1 (Forge 47.4.10). Focused on experimental terrain
generation: custom noise, Wave Function Collapse, PNG-based cave generation for D&D
dungeon maps, and custom biomes/structures/blocks/entities.

- **Mod ID:** `cruikshank`
- **Package:** `com.cruikshank.mod`
- **Java:** 17+ (developed with Oracle JDK 21)
- **IDE:** IntelliJ IDEA with Minecraft Development plugin

## Project Structure

```
src/main/java/com/cruikshank/mod/
    CruikshankMod.java              Main mod class, event handlers, registry
    world/
        CruikshankFlatChunkGenerator.java   Ore Columns generator
        SuperflatChunkGenerator.java        Superflat 1 generator
        LosAngelesChunkGenerator.java       Los Angeles generator

src/main/resources/
    assets/cruikshank/lang/en_us.json       Translation keys
    data/cruikshank/
        dimension/cruikshank_flat.json      Custom dimension (manual /execute in access)
        worldgen/world_preset/
            ore_columns.json                Ore Columns world type
            superflat_1.json                Superflat 1 world type
            los_angeles.json                Los Angeles world type
    data/minecraft/
        tags/worldgen/world_preset/normal.json   Adds all 3 world types to creation screen
        worldgen/structure_set/villages.json      Dense villages (global override)
```

## Architecture

### World Types

Three world types appear in the "World Type" button on the world creation screen:

**"Ore Columns"** (`CruikshankFlatChunkGenerator`) — Extends `NoiseBasedChunkGenerator`.
Vanilla noise terrain generates first via `super`, then `replaceStoneWithOreBands()` runs
in `.thenApply()` on the `fillFromNoise` future. Scans each column top-down from `maxY`,
finds the first non-air block (the surface), then replaces solid blocks with LAYERS entries
counting down from the top layer. Currently limited to one column (x < 1, z < 1) for testing.

**"Superflat 1"** (`SuperflatChunkGenerator`) — Extends `NoiseBasedChunkGenerator` but does
NOT call super in `fillFromNoise`. Fills every column identically with flat layers from
bottom to top: bedrock, obsidian, ore blocks, stone filler, dirt, grass_block. Surface at
Y=~1 (depends on layer count). No caves (applyCarvers is no-op). Structures, trees, and
mobs spawn via inherited biome features.

**"Los Angeles"** (`LosAngelesChunkGenerator`) — Minimal wrapper extending
`NoiseBasedChunkGenerator`, overrides nothing except `codec()`. Pure vanilla terrain.
Exists as a distinct generator type for future LA-specific behavior.

All three use `multi_noise` overworld biome source and vanilla overworld noise settings.
Nether and End use standard vanilla generators in all presets.

### Generator Codec Pattern

Each generator has:
- `public static final Codec<...> CODEC` using `RecordCodecBuilder` with `biome_source` + `settings`
- Constructor taking `BiomeSource` + `Holder<NoiseGeneratorSettings>`
- `codec()` override returning the CODEC
- Registration in `CruikshankMod.java` static block: `CHUNK_GENERATORS.register("name", () -> Generator.CODEC)`

### Custom Dimension

`cruikshank:cruikshank_flat` dimension exists separately from the world types. Accessible
via `/execute in cruikshank:cruikshank_flat run tp @s ~ 60 ~` from any world.

### Dense Villages

`data/minecraft/worldgen/structure_set/villages.json` overrides vanilla's village placement
globally (spacing: 4, separation: 2, all 5 village types). Affects ALL world types when
the mod is loaded. Can be disabled by renaming to `.txt`.

### Event Handlers (CruikshankMod.java)

- `onServerStarting` — log line for startup confirmation.

## Build & Run

| Action | Command |
|---|---|
| Run client | `.\gradlew runClient` or IntelliJ runClient config |
| Build jar | `.\gradlew build` |
| Regen run configs | `.\gradlew genIntellijRuns` |

## How to Add a New World Type

Each world type needs these pieces:

1. **Java class** — a ChunkGenerator subclass in `com.cruikshank.mod.world`
   - Must have a `public static final Codec` field
   - Must override `codec()` to return it
   - Extend `NoiseBasedChunkGenerator` for vanilla terrain + modifications
   - Extend `ChunkGenerator` directly for fully custom terrain
2. **Codec registration** — in `CruikshankMod.java`, add to `CHUNK_GENERATORS`:
   `CHUNK_GENERATORS.register("my_generator", () -> MyGenerator.CODEC);`
3. **WorldPreset JSON** — `data/cruikshank/worldgen/world_preset/my_preset.json`
   Must define all 3 dimensions (overworld, nether, end). Generator type is
   `"cruikshank:my_generator"` matching the codec registration name.
4. **Tag entry** — append to `data/minecraft/tags/worldgen/world_preset/normal.json`
   to make it appear in the world creation "World Type" button.
5. **Translation** — add `"generator.cruikshank.my_preset": "My Name"` to
   `assets/cruikshank/lang/en_us.json`

Generators can share code via a base class or utility methods.

## Current State (active development)

Three world types registered. CruikshankFlatChunkGenerator (Ore Columns) has its loop
limited to one column (x < 1, z < 1) for testing. The `isPlains()` biome check is
commented out so replacement runs in all biomes. SuperflatChunkGenerator uses a Layer
record pattern similar to Ore Columns. LosAngelesChunkGenerator is a vanilla pass-through.

MDK template boilerplate (Config.java, example blocks/items/tabs) has been removed.
Empty DeferredRegisters for BLOCKS, ITEMS, CREATIVE_MODE_TABS are kept for future use.

## Development Notes

- Debug logging is enabled: `forge.logging.console.level=debug` in build.gradle
- Gradle daemon and caching are enabled in gradle.properties
- The `run/` directory contains the dev Minecraft instance (saves, logs, configs)
- The user develops on Windows with IntelliJ IDEA; Claude edits via the GitHub repo
- After Claude pushes, user pulls with `git pull` on Windows
- User prefers console logging for debugging
- Do NOT rewrite the user's code without asking — make targeted changes only

## Future Plans

- "Noise terrain + layers" world type: vanilla noise terrain with flat ore layers banded through it
- PNG-to-cave generation (read dungeon map images, generate caves from pixel data)
- Wave Function Collapse terrain generation
- Custom biomes, blocks, items, entities
