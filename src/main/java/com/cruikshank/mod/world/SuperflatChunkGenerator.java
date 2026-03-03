package com.cruikshank.mod.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SuperflatChunkGenerator extends NoiseBasedChunkGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<SuperflatChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.noiseSettings)
            ).apply(instance, SuperflatChunkGenerator::new)
    );

    private final Holder<NoiseGeneratorSettings> noiseSettings;

    private record Layer(Block block, int height) {}

    private static final List<Layer> LAYERS = List.of(
            new Layer(Blocks.BEDROCK, 1),
            new Layer(Blocks.OBSIDIAN, 1),
            new Layer(Blocks.REDSTONE_BLOCK, 2),
            new Layer(Blocks.LAPIS_BLOCK, 2),
            new Layer(Blocks.GOLD_BLOCK, 2),
            new Layer(Blocks.EMERALD_BLOCK, 2),
            new Layer(Blocks.DIAMOND_BLOCK, 2),
            new Layer(Blocks.COPPER_BLOCK, 2),
            new Layer(Blocks.BONE_BLOCK, 2),
            new Layer(Blocks.IRON_BLOCK, 2),
            new Layer(Blocks.COAL_BLOCK, 2),
            new Layer(Blocks.STONE, 40),
            new Layer(Blocks.DIRT, 4),
            new Layer(Blocks.GRASS_BLOCK, 1)
    );

    // Total height of all layers
    private static final int TOTAL_LAYER_HEIGHT;
    // Pre-computed flat column of block states
    private static final BlockState[] COLUMN;
    // Surface Y level (absolute)
    private static final int SURFACE_Y;

    static {
        int total = 0;
        for (Layer layer : LAYERS) {
            total += layer.height;
        }
        TOTAL_LAYER_HEIGHT = total;

        // Build from minY (-64) up
        // Surface will be at -64 + TOTAL_LAYER_HEIGHT - 1
        SURFACE_Y = -64 + TOTAL_LAYER_HEIGHT;

        COLUMN = new BlockState[TOTAL_LAYER_HEIGHT];
        int idx = 0;
        for (Layer layer : LAYERS) {
            for (int i = 0; i < layer.height; i++) {
                COLUMN[idx++] = layer.block.defaultBlockState();
            }
        }

        LOGGER.info("Cruikshank Superflat: {} layers, {} total blocks, surface at Y={}", LAYERS.size(), TOTAL_LAYER_HEIGHT, SURFACE_Y);
    }

    public SuperflatChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.noiseSettings = settings;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                         RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        // Don't call super — we fill everything ourselves
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int i = 0; i < TOTAL_LAYER_HEIGHT; i++) {
                    int y = minY + i;
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, COLUMN[i], false);
                }
            }
        }

        // Update heightmaps so structures and spawning work correctly
        Heightmap.primeHeightmaps(chunk, EnumSet.allOf(Heightmap.Types.class));

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        // No-op: we place our own grass/dirt in fillFromNoise
    }

    @Override
    public void applyCarvers(RandomState randomState, long seed, net.minecraft.world.level.biome.BiomeManager biomeManager,
                              StructureManager structureManager, ChunkAccess chunk, net.minecraft.world.level.levelgen.GenerationStep.Carving carving) {
        // No-op: no caves in superflat
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, net.minecraft.world.level.LevelHeightAccessor level, RandomState randomState) {
        return SURFACE_Y;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, net.minecraft.world.level.LevelHeightAccessor level, RandomState randomState) {
        int minY = level.getMinBuildHeight();
        int height = level.getHeight();
        BlockState[] states = new BlockState[height];

        // Fill with air by default
        for (int i = 0; i < height; i++) {
            states[i] = Blocks.AIR.defaultBlockState();
        }

        // Place our flat layers starting from the bottom
        for (int i = 0; i < TOTAL_LAYER_HEIGHT && i < height; i++) {
            states[i] = COLUMN[i];
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        super.addDebugScreenInfo(info, randomState, pos);
        info.add("Cruikshank Superflat 1");
    }
}
