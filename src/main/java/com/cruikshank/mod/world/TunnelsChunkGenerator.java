package com.cruikshank.mod.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TunnelsChunkGenerator extends NoiseBasedChunkGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<TunnelsChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.noiseSettings)
            ).apply(instance, TunnelsChunkGenerator::new)
    );

    private final Holder<NoiseGeneratorSettings> noiseSettings;

    private static final int TUNNEL_DEPTH = 20;
    private static final int TUNNEL_HALF_WIDTH = 1; // 3-wide: center ± 1
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState GLOWSTONE = Blocks.GLOWSTONE.defaultBlockState();

    public TunnelsChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
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
        return super.fillFromNoise(executor, blender, randomState, structureManager, chunk)
                .thenApply(c -> {
                    carveTunnels(c);
                    return c;
                });
    }

    private int findSurfaceY(ChunkAccess chunk, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int maxY = chunk.getMaxBuildHeight();
        int minY = chunk.getMinBuildHeight();

        for (int y = maxY - 1; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.WATER)) {
                return y;
            }
        }
        return minY;
    }

    private void carveTunnels(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // North-south tunnel: runs along z at local x=7..9
        for (int z = 0; z < 16; z++) {
            // Use x=8 (center of tunnel) for surface detection
            int surfaceY = findSurfaceY(chunk, 8, z);
            int centerY = surfaceY - TUNNEL_DEPTH;

            for (int x = 8 - TUNNEL_HALF_WIDTH; x <= 8 + TUNNEL_HALF_WIDTH; x++) {
                // Carve 3-tall air
                for (int y = centerY - TUNNEL_HALF_WIDTH; y <= centerY + TUNNEL_HALF_WIDTH; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, AIR, false);
                }
                // Glowstone ceiling
                pos.set(x, centerY + TUNNEL_HALF_WIDTH + 1, z);
                chunk.setBlockState(pos, GLOWSTONE, false);
            }
        }

        // East-west tunnel: runs along x at local z=7..9
        for (int x = 0; x < 16; x++) {
            // Use z=8 (center of tunnel) for surface detection
            int surfaceY = findSurfaceY(chunk, x, 8);
            int centerY = surfaceY - TUNNEL_DEPTH;

            for (int z = 8 - TUNNEL_HALF_WIDTH; z <= 8 + TUNNEL_HALF_WIDTH; z++) {
                // Carve 3-tall air
                for (int y = centerY - TUNNEL_HALF_WIDTH; y <= centerY + TUNNEL_HALF_WIDTH; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, AIR, false);
                }
                // Glowstone ceiling
                pos.set(x, centerY + TUNNEL_HALF_WIDTH + 1, z);
                chunk.setBlockState(pos, GLOWSTONE, false);
            }
        }
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        super.addDebugScreenInfo(info, randomState, pos);
        info.add("Cruikshank (Tunnels)");
    }
}
