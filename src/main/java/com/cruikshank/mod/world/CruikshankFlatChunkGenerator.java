package com.cruikshank.mod.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
//            new Layer(Blocks.BEDROCK, 1),
//            new Layer(Blocks.OBSIDIAN, 1),
//            new Layer(Blocks.STONE, 65),
//            new Layer(Blocks.LAVA, 1),
//            new Layer(Blocks.STONE, 1),
//            new Layer(Blocks.SAND, 2),
//            new Layer(Blocks.STONE, 1),
//            new Layer(Blocks.WATER, 1),
//            new Layer(Blocks.STONE, 1),
//            new Layer(Blocks.SHROOMLIGHT, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.REDSTONE_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.LAPIS_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.GOLD_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.EMERALD_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            //new Layer(Blocks.GRAVEL, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.DIAMOND_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.COPPER_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.BONE_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.IRON_BLOCK, 2),
            //new Layer(Blocks.STONE, 1),
            new Layer(Blocks.COAL_BLOCK, 2)
            //new Layer(Blocks.STONE, 4),
            //new Layer(Blocks.OAK_LOG, 4),
            //new Layer(Blocks.DIRT, 2),
            //new Layer(Blocks.GRASS_BLOCK, 1)
    );

    // Y offset from minBuildHeight -> replacement block (only non-stone entries)
    private static final Map<Integer, BlockState> REPLACEMENTS;

    static {
        REPLACEMENTS = new HashMap<>();
        int y = 0;
        for (Layer layer : LAYERS) {
            for (int i = 0; i < layer.height; i++) {
                if (layer.block != Blocks.STONE) {
                    REPLACEMENTS.put(y, layer.block.defaultBlockState());
                }
                y++;
            }
        }
        LOGGER.info("Cruikshank: {} ore band replacements across {} layers", REPLACEMENTS.size(), LAYERS.size());
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
                    //if (isPlains(c)) {
                        replaceStoneWithOreBands(c);
                    //}
                    return c;
                });
    }

    private void replaceStoneWithOreBands(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 1; x++) {
            for (int z = 0; z < 1; z++) {
                int layerIndex = REPLACEMENTS.size() - 1;
                boolean foundSurface = false;

                for (int y = maxY - 1; y >= minY; y--) {
                    pos.set(x, y, z);
                    BlockState existing = chunk.getBlockState(pos);

                    if (!foundSurface) {
                        if (existing.isAir()) continue;
                        foundSurface = true;
                    }

                    if (layerIndex < 0) break;

                    if (existing.is(Blocks.WATER) || existing.is(Blocks.LAVA) || existing.isAir()) {
                        layerIndex--; // skip but still consume a layer
                    } else {
                        chunk.setBlockState(pos, REPLACEMENTS.get(layerIndex), false);
                        layerIndex--;
                    }
                }
            }
        }
    }

    /*
    private void replaceStoneWithOreBands(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        int replaced = 0;

        for (int x = 0; x < 1; x++) {
            for (int z = 0; z < 1; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    if (!chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultBlockState(), false);
                        replaced++;
                    }
                }
            }
        }

        LOGGER.info("Cruikshank: chunk {} — {} blocks replaced with diamond", chunk.getPos(), replaced);
    }

    private void replaceStoneWithOreBands(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();

        for (int x = 0; x < 1; x++) {
            for (int z = 0; z < 1; z++) {
                for (var entry : REPLACEMENTS.entrySet()) {
                    pos.set(x, minY + entry.getKey(), z);
                    if (!chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, entry.getValue(), false);
                    }
                }
            }
        }
    }
    private void replaceStoneWithOreBands(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (var entry : REPLACEMENTS.entrySet()) {
                    pos.set(x, minY + entry.getKey(), z);
                    if (chunk.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                        chunk.setBlockState(pos, entry.getValue(), false);
                    }
                }
            }
        }
    }

    private void replaceStoneWithOreBands(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        int replaced = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    if (chunk.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                        chunk.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultBlockState(), false);
                        replaced++;
                    }
                }
            }
        }

        LOGGER.info("Cruikshank: chunk {} — {} blocks replaced with diamond", chunk.getPos(), replaced);
    }
   */

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        super.addDebugScreenInfo(info, randomState, pos);
        info.add("Cruikshank (Plains = ore bands)");
    }
}