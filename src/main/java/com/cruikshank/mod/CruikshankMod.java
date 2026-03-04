package com.cruikshank.mod;

import com.cruikshank.mod.block.CotBlock;
import com.cruikshank.mod.block.EmeraldChestBlock;
import com.cruikshank.mod.block.EmeraldChestBlockEntity;
import com.cruikshank.mod.client.EmeraldGolemRenderer;
import com.cruikshank.mod.entity.EmeraldGolem;
import com.cruikshank.mod.sound.ModSounds;
import com.cruikshank.mod.spell.SpellHandler;
import com.cruikshank.mod.world.CruikshankFlatChunkGenerator;
import com.cruikshank.mod.world.LosAngelesChunkGenerator;
import com.cruikshank.mod.world.SuperflatChunkGenerator;
import com.cruikshank.mod.world.SkyCruikshankChunkGenerator;
import com.cruikshank.mod.world.TunnelsChunkGenerator;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(CruikshankMod.MODID)
public class CruikshankMod
{
    public static final String MODID = "cruikshank";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);

    public static final RegistryObject<Block> WHITE_COT = BLOCKS.register("white_cot",
            () -> new CotBlock(DyeColor.WHITE,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).sound(SoundType.WOOL).strength(0.2F)));

    public static final RegistryObject<Item> WHITE_COT_ITEM = ITEMS.register("white_cot",
            () -> new BlockItem(WHITE_COT.get(), new Item.Properties()));

    public static final RegistryObject<Item> GOLDEN_MUSIC_DISC = ITEMS.register("golden_music_disc",
            () -> new RecordItem(6, ModSounds.GOLDEN, new Item.Properties().stacksTo(1), 3841));

    // Emerald Chest
    public static final RegistryObject<Block> EMERALD_CHEST = BLOCKS.register("emerald_chest",
            () -> new EmeraldChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.EMERALD)
                    .sound(SoundType.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> EMERALD_CHEST_ITEM = ITEMS.register("emerald_chest",
            () -> new BlockItem(EMERALD_CHEST.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<EmeraldChestBlockEntity>> EMERALD_CHEST_BE =
            BLOCK_ENTITY_TYPES.register("emerald_chest",
                    () -> BlockEntityType.Builder.of(EmeraldChestBlockEntity::new, EMERALD_CHEST.get()).build(null));

    // Emerald Golem
    public static final RegistryObject<EntityType<EmeraldGolem>> EMERALD_GOLEM =
            ENTITY_TYPES.register("emerald_golem",
                    () -> EntityType.Builder.<EmeraldGolem>of(EmeraldGolem::new, MobCategory.MISC)
                            .sized(0.6F, 1.1F)
                            .clientTrackingRange(8)
                            .build("emerald_golem"));

    static {
        CHUNK_GENERATORS.register("cruikshank_flat", () -> CruikshankFlatChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("superflat_1", () -> SuperflatChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("los_angeles", () -> LosAngelesChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("tunnels", () -> TunnelsChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("sky_cruikshank", () -> SkyCruikshankChunkGenerator.CODEC);
    }

    public CruikshankMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        // MinecraftForge.EVENT_BUS.register(new SpellHandler());

        CHUNK_GENERATORS.register(modEventBus);

        ModSounds.SOUND_EVENTS.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("CRUIKSHANK: COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("CRUIKSHANK: server starting");
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null && overworld.getChunkSource().getGenerator() instanceof SkyCruikshankChunkGenerator) {
            overworld.setDefaultSpawnPos(new BlockPos(1, 71, 3), 0.0F);
            LOGGER.info("CRUIKSHANK: set Sky Cruikshank spawn to island");
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event)
    {
        if (event.getEntity() == null || event.getEntity().level().isClientSide()) return;

        BlockPos placedPos = event.getPos();
        var level = (ServerLevel) event.getEntity().level();

        // Check if carved pumpkin was placed on top of an emerald block
        if (!event.getPlacedBlock().is(Blocks.CARVED_PUMPKIN)) return;

        BlockPos belowPos = placedPos.below();
        if (!level.getBlockState(belowPos).is(Blocks.EMERALD_BLOCK)) return;

        LOGGER.info("CRUIKSHANK: Emerald Golem construction detected at {}", belowPos);

        // Replace emerald block with emerald chest
        level.setBlock(belowPos, EMERALD_CHEST.get().defaultBlockState(), 3);

        // Remove the pumpkin
        level.setBlock(placedPos, Blocks.AIR.defaultBlockState(), 3);

        // Spawn the golem on top of the chest
        EmeraldGolem golem = EMERALD_GOLEM.get().create(level);
        if (golem != null) {
            golem.setPos(belowPos.getX() + 0.5, belowPos.getY() + 1.0, belowPos.getZ() + 0.5);
            golem.setChestPos(belowPos);
            level.addFreshEntity(golem);
            LOGGER.info("CRUIKSHANK: Spawned Emerald Golem at {}", golem.position());
        }
    }

    private static final String TUNNELS_PICKAXE_TAG = "cruikshank_tunnels_pickaxe";

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        var player = event.getEntity();
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        if (!(generator instanceof TunnelsChunkGenerator)) return;

        CompoundTag persisted = player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(TUNNELS_PICKAXE_TAG)) return;

        persisted.putBoolean(TUNNELS_PICKAXE_TAG, true);
        player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);

        player.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        LOGGER.info("CRUIKSHANK: gave iron pickaxe to {} (Tunnels world)", player.getName().getString());
    }

    @SubscribeEvent
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event)
    {
        BlockPos pos = event.getNewSpawn();
        if (pos == null) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        ServerLevel level = serverPlayer.server.getLevel(event.getSpawnLevel());
        if (level == null) return;

        if (level.getBlockState(pos).getBlock() instanceof CotBlock) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAnimalTamed(AnimalTameEvent event)
    {
        if (!(event.getAnimal() instanceof Wolf wolf)) return;
        wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                ModSounds.WOLF_TAMED.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getTarget() instanceof Wolf wolf)) return;
        if (!wolf.isTame() || !wolf.isOwnedBy(event.getEntity())) return;

        Player player = event.getEntity();
        if (wolf.isFood(player.getItemInHand(event.getHand())) && wolf.getHealth() < wolf.getMaxHealth()) {
            wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                    ModSounds.WOLF_FEED.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else if (!wolf.isFood(player.getItemInHand(event.getHand())) && !wolf.isInSittingPose()) {
            wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                    ModSounds.WOLF_TOLD_TO_SIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static final double WOLF_HEAR_RANGE = 24.0;

    @SubscribeEvent
    public void onPlayerLevelUp(PlayerXpEvent.LevelChange event)
    {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getLevels() <= 0) return;

        Player player = event.getEntity();
        AABB area = player.getBoundingBox().inflate(WOLF_HEAR_RANGE);
        var wolves = player.level().getEntitiesOfClass(Wolf.class, area,
                w -> w.isTame() && w.isOwnedBy(player));

        if (!wolves.isEmpty()) {
            Wolf wolf = wolves.get(player.level().random.nextInt(wolves.size()));
            wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                    ModSounds.WOLF_NEARBY_PLAYER_LEVEL_UP.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onPlayerSleep(PlayerSleepInBedEvent event)
    {
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        AABB area = player.getBoundingBox().inflate(WOLF_HEAR_RANGE);
        var wolves = player.level().getEntitiesOfClass(Wolf.class, area,
                w -> w.isTame() && w.isOwnedBy(player));

        if (!wolves.isEmpty()) {
            Wolf wolf = wolves.get(player.level().random.nextInt(wolves.size()));
            wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                    ModSounds.WOLF_NEARBY_PLAYER_SLEEP.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("CRUIKSHANK: CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerEntityRenderer(EMERALD_GOLEM.get(), EmeraldGolemRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents
    {
        @SubscribeEvent
        public static void onEntityAttributeCreation(EntityAttributeCreationEvent event)
        {
            event.put(EMERALD_GOLEM.get(), EmeraldGolem.createAttributes().build());
        }
    }
}
