package com.cruikshank.mod.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SkyCruikshankChunkGenerator extends NoiseBasedChunkGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<SkyCruikshankChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.noiseSettings)
            ).apply(instance, SkyCruikshankChunkGenerator::new)
    );

    private final Holder<NoiseGeneratorSettings> noiseSettings;

    private static final int ISLAND_Y = 70;
    private static final int DIRT_DEPTH = 3;
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState OAK_LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState OAK_LEAVES = Blocks.OAK_LEAVES.defaultBlockState();
    private static final BlockState CHEST = Blocks.CHEST.defaultBlockState();

    // L-shaped island blocks (x, z) — vertical arm + horizontal arm
    // Vertical arm: x=0..2, z=0..4
    // Horizontal arm: x=3..6, z=3..4
    private static boolean isIsland(int x, int z) {
        return (x >= 0 && x <= 2 && z >= 0 && z <= 4)
            || (x >= 3 && x <= 6 && z >= 3 && z <= 4);
    }

    public SkyCruikshankChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
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
        ChunkPos cp = chunk.getPos();
        if (cp.x != 0 || cp.z != 0) {
            return CompletableFuture.completedFuture(chunk);
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Place L-shaped island: grass on top, dirt below
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!isIsland(x, z)) continue;

                pos.set(x, ISLAND_Y, z);
                chunk.setBlockState(pos, GRASS, false);
                for (int d = 1; d < DIRT_DEPTH; d++) {
                    pos.set(x, ISLAND_Y - d, z);
                    chunk.setBlockState(pos, DIRT, false);
                }
            }
        }

        // Place tree at (1, ISLAND_Y+1, 1)
        int treeX = 1, treeZ = 1;
        int trunkBase = ISLAND_Y + 1;
        int trunkHeight = 4;
        for (int i = 0; i < trunkHeight; i++) {
            pos.set(treeX, trunkBase + i, treeZ);
            chunk.setBlockState(pos, OAK_LOG, false);
        }

        // Leaves: two 3x3 layers, then a 1x3 cross on top
        int leafBase = trunkBase + trunkHeight - 2; // start leaves 2 below top of trunk
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int lx = treeX + dx;
                    int lz = treeZ + dz;
                    if (lx < 0 || lz < 0) continue;
                    pos.set(lx, leafBase + dy, lz);
                    if (chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, OAK_LEAVES, false);
                    }
                }
            }
        }
        // Top cross
        int topY = leafBase + 2;
        for (int[] offset : new int[][]{{0,0}, {-1,0}, {1,0}, {0,-1}, {0,1}}) {
            int lx = treeX + offset[0];
            int lz = treeZ + offset[1];
            if (lx < 0 || lz < 0) continue;
            pos.set(lx, topY, lz);
            chunk.setBlockState(pos, OAK_LEAVES, false);
        }

        // Place chest at (5, ISLAND_Y+1, 4) with lava and water buckets
        int chestX = 5, chestZ = 4;
        pos.set(chestX, ISLAND_Y + 1, chestZ);
        chunk.setBlockState(pos, CHEST, false);
        BlockEntity be = chunk.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.LAVA_BUCKET));
            chest.setItem(1, new ItemStack(Items.WATER_BUCKET));
        }

        Heightmap.primeHeightmaps(chunk, EnumSet.allOf(Heightmap.Types.class));
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(net.minecraft.server.level.WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // No-op
    }

    @Override
    public void applyCarvers(net.minecraft.server.level.WorldGenRegion region, long seed, RandomState randomState,
                              net.minecraft.world.level.biome.BiomeManager biomeManager, StructureManager structureManager,
                              ChunkAccess chunk, net.minecraft.world.level.levelgen.GenerationStep.Carving carving) {
        // No-op
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, net.minecraft.world.level.LevelHeightAccessor level, RandomState randomState) {
        if (isIsland(x, z)) return ISLAND_Y + 1;
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, net.minecraft.world.level.LevelHeightAccessor level, RandomState randomState) {
        int minY = level.getMinBuildHeight();
        int height = level.getHeight();
        BlockState[] states = new BlockState[height];
        for (int i = 0; i < height; i++) {
            states[i] = Blocks.AIR.defaultBlockState();
        }
        if (isIsland(x, z)) {
            for (int d = 0; d < DIRT_DEPTH - 1; d++) {
                int idx = (ISLAND_Y - (DIRT_DEPTH - 1) + d) - minY;
                if (idx >= 0 && idx < height) states[idx] = DIRT;
            }
            int topIdx = ISLAND_Y - minY;
            if (topIdx >= 0 && topIdx < height) states[topIdx] = GRASS;
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        super.addDebugScreenInfo(info, randomState, pos);
        info.add("Sky Cruikshank");
    }
}
