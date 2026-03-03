package com.cruikshank.mod.world;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CruikshankFlatChunkGenerator extends ChunkGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Codec tells Minecraft how to serialize/deserialize this generator (for saving worlds)
    public static final Codec<CruikshankFlatChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, CruikshankFlatChunkGenerator::new)
    );

    // Layer definition: block + how many of that block
    private record Layer(Block block, int height) {}

    // Your Bedrock flat world layers, bottom to top
    private static final List<Layer> LAYERS = List.of(
            new Layer(Blocks.BEDROCK, 1),
            new Layer(Blocks.OBSIDIAN, 1),
            new Layer(Blocks.STONE, 65),
            new Layer(Blocks.LAVA, 1),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.SAND, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.WATER, 1),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.SHROOMLIGHT, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.REDSTONE_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.LAPIS_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.GOLD_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.EMERALD_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.GRAVEL, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.DIAMOND_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.COPPER_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.BONE_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.IRON_BLOCK, 2),
            new Layer(Blocks.STONE, 1),
            new Layer(Blocks.COAL_BLOCK, 2),
            new Layer(Blocks.STONE, 4),
            new Layer(Blocks.OAK_LOG, 4),
            new Layer(Blocks.DIRT, 2),
            new Layer(Blocks.GRASS_BLOCK, 1)
    );

    // Pre-computed column of block states for fast chunk filling
    private static final int TOTAL_HEIGHT;
    private static final BlockState[] COLUMN;

    static {
        TOTAL_HEIGHT = LAYERS.stream().mapToInt(Layer::height).sum();
        COLUMN = new BlockState[TOTAL_HEIGHT];
        int y = 0;
        for (Layer layer : LAYERS) {
            BlockState state = layer.block.defaultBlockState();
            for (int i = 0; i < layer.height; i++) {
                COLUMN[y++] = state;
            }
        }
        // TOTAL_HEIGHT = 117, surface (grass_block) at y = -64 + 116 = 52
    }

    public CruikshankFlatChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        LOGGER.info("Cruikshank flat generator created! {} layers, {} blocks tall", LAYERS.size(), TOTAL_HEIGHT);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight(); // -64 in 1.20.1

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int i = 0; i < TOTAL_HEIGHT; i++) {
                    chunk.setBlockState(pos.set(x, minY + i, z), COLUMN[i], false);
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        // No surface modifications needed — our layers are the surface
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                             LevelHeightAccessor level, RandomState randomState) {
        // One above the top solid block (grass at y=52, so return 53)
        return level.getMinBuildHeight() + TOTAL_HEIGHT;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(level.getMinBuildHeight(), COLUMN.clone());
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Cruikshank Flat Generator");
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
                                     StructureManager structureManager) {
        /*
        // Trees, flowers, but no ores
        ChunkPos chunkPos = chunk.getPos();
        BlockPos origin = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());

        Holder<Biome> biome = level.getBiome(origin.offset(8, 0, 8));
        List<HolderSet<PlacedFeature>> featuresByStep = biome.value().getGenerationSettings().features();

        // Only apply VEGETAL_DECORATION (trees, flowers, grass) — skip ores, lakes, etc.
        int vegetalIndex = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        if (vegetalIndex < featuresByStep.size()) {
            for (Holder<PlacedFeature> feature : featuresByStep.get(vegetalIndex)) {
                try {
                    feature.value().place(level, this, level.getRandom(), origin);
                } catch (Exception e) {
                    LOGGER.warn("Decoration failed: {}", e.getMessage());
                }
            }
        }*/
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        net.minecraft.world.level.NaturalSpawner.spawnMobsForChunkGeneration(
                region, region.getBiome(region.getCenter().getWorldPosition()), region.getCenter(), region.getRandom()
        );
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             net.minecraft.world.level.biome.BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk,
                             net.minecraft.world.level.levelgen.GenerationStep.Carving carving) {
        // No caves or ravines in flat world
    }
}