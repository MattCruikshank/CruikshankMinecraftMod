package com.cruikshank.mod.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CruikshankFlatChunkGenerator extends NoiseBasedChunkGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<CruikshankFlatChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.noiseSettings)
            ).apply(instance, CruikshankFlatChunkGenerator::new)
    );

    private final Holder<NoiseGeneratorSettings> noiseSettings;

    private record Layer(Block block, int height) {}

    private static final List<Layer> LAYERS = List.of(
            new Layer(Blocks.REDSTONE_BLOCK, 2),
            new Layer(Blocks.LAPIS_BLOCK, 2),
            new Layer(Blocks.GOLD_BLOCK, 2),
            new Layer(Blocks.EMERALD_BLOCK, 2),
            new Layer(Blocks.DIAMOND_BLOCK, 2),
            new Layer(Blocks.COPPER_BLOCK, 2),
            new Layer(Blocks.BONE_BLOCK, 2),
            new Layer(Blocks.IRON_BLOCK, 2),
            new Layer(Blocks.COAL_BLOCK, 2)
    );

    // Expanded layer blocks array — each entry is one Y level, bottom to top
    private static final BlockState[] LAYER_BLOCKS;

    static {
        List<BlockState> blocks = new ArrayList<>();
        for (Layer layer : LAYERS) {
            for (int i = 0; i < layer.height; i++) {
                blocks.add(layer.block.defaultBlockState());
            }
        }
        LAYER_BLOCKS = blocks.toArray(new BlockState[0]);
        LOGGER.info("Cruikshank: {} replacement blocks from {} layers", LAYER_BLOCKS.length, LAYERS.size());
    }

    public CruikshankFlatChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.noiseSettings = settings;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private boolean isPlains(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        return chunk.getNoiseBiome(
                QuartPos.fromBlock(cp.getMiddleBlockX()),
                QuartPos.fromBlock(0),
                QuartPos.fromBlock(cp.getMiddleBlockZ())
        ).is(Biomes.PLAINS);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return super.fillFromNoise(executor, blender, randomState, structureManager, chunk)
                .thenApply(c -> {
                    if (isPlains(c)) {
                        replaceStoneWithOreBands(c);
                    }
                    return c;
                });
    }

    private void replaceStoneWithOreBands(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int layerIndex = LAYER_BLOCKS.length - 1;
                boolean foundSurface = false;

                for (int y = maxY - 1; y >= minY; y--) {
                    pos.set(x, y, z);
                    BlockState existing = chunk.getBlockState(pos);

                    if (!foundSurface) {
                        if (existing.isAir()) continue;
                        foundSurface = true;
                    }

                    if (layerIndex < 0) break;

                    if (existing.isAir() || existing.is(Blocks.WATER) || existing.is(Blocks.LAVA)) {
                        layerIndex--; // skip but still consume a layer
                    } else {
                        chunk.setBlockState(pos, LAYER_BLOCKS[layerIndex], false);
                        layerIndex--;
                    }
                }
            }
        }
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        super.addDebugScreenInfo(info, randomState, pos);
        info.add("Cruikshank (Plains = ore bands)");
    }
}
