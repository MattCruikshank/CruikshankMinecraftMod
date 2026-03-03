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
    Config.java                     Forge config (from MDK template)
    world/
        CruikshankFlatChunkGenerator.java   Custom chunk generator

src/main/resources/
    assets/cruikshank/lang/en_us.json       Translation keys
    data/cruikshank/
        dimension/cruikshank_flat.json      Custom dimension (teleportable)
        worldgen/
            world_preset/cruikshank_flat.json   World type for world creation screen
            structure_set/villages.txt       Dense villages (renamed to .txt to disable)
    data/minecraft/tags/worldgen/
        world_preset/normal.json            Adds Cruikshank Flat to world type list
```

## Architecture

### CruikshankFlatChunkGenerator

Extends `NoiseBasedChunkGenerator`. Vanilla noise terrain generates first via `super`,
then `replaceStoneWithOreBands()` runs in `.thenApply()` on the `fillFromNoise` future.

Current behavior: scans each column top-down from `maxY`, finds the first non-air block
(the surface), then replaces solid blocks with LAYERS entries counting down from the top
layer. Skips air/water/lava but still consumes the layer index.

The LAYERS list and REPLACEMENTS map drive the ore band pattern. REPLACEMENTS is keyed
by sequential index (0, 1, 2...) with stone layers excluded.

Biome check `isPlains()` is available but currently commented out in `fillFromNoise` —
replacement runs in all biomes during development.

### World Types

**"Cruikshank Flat"** — registered via JSON WorldPreset + normal tag. Uses
`cruikshank:cruikshank_flat` generator with `multi_noise` overworld biome source and
vanilla overworld noise settings. Nether and End use standard vanilla generators.

### Custom Dimension

`cruikshank:cruikshank_flat` dimension exists separately from the world type. Accessible
via `/execute in cruikshank:cruikshank_flat run tp @s ~ 60 ~` from any world.

### Event Handlers (CruikshankMod.java)

- `onPlayerLogin` / `onPlayerRespawn` — redirects players to the cruikshank_flat
  dimension if the current world doesn't use CruikshankFlatChunkGenerator.
- `onServerStarting` — log line for startup confirmation.

### Dense Villages (disabled)

`villages.txt` in `structure_set/` — rename to `.json` to enable dense village placement
(spacing: 2, separation: 1). Affects all worlds globally when active.

## Build & Run

| Action | Command |
|---|---|
| Run client | `.\gradlew runClient` or IntelliJ runClient config |
| Build jar | `.\gradlew build` |
| Regen run configs | `.\gradlew genIntellijRuns` |

## Development Notes

- Debug logging is enabled: `forge.logging.console.level=debug` in build.gradle
- Gradle daemon and caching are enabled in gradle.properties
- The `run/` directory contains the dev Minecraft instance (saves, logs, configs)
- Example blocks/items from MDK template still present in CruikshankMod.java
- Multiple world types planned — use separate ChunkGenerator classes per world type,
  each with their own codec registration, WorldPreset JSON, and lang entry

## Future Plans

- PNG-to-cave generation (read dungeon map images, generate caves from pixel data)
- Wave Function Collapse terrain generation
- Custom biomes, blocks, items, entities
- Multiple world type presets for different generation experiments
