package com.cruikshank.mod;

import com.cruikshank.mod.sound.ModSounds;
import com.cruikshank.mod.spell.SpellHandler;
import com.cruikshank.mod.world.CruikshankFlatChunkGenerator;
import com.cruikshank.mod.world.LosAngelesChunkGenerator;
import com.cruikshank.mod.world.SuperflatChunkGenerator;
import com.cruikshank.mod.world.TunnelsChunkGenerator;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);

    public static final RegistryObject<Item> GOLDEN_MUSIC_DISC = ITEMS.register("golden_music_disc",
            () -> new RecordItem(6, ModSounds.GOLDEN, new Item.Properties().stacksTo(1), 3841));

    static {
        CHUNK_GENERATORS.register("cruikshank_flat", () -> CruikshankFlatChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("superflat_1", () -> SuperflatChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("los_angeles", () -> LosAngelesChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("tunnels", () -> TunnelsChunkGenerator.CODEC);
    }

    public CruikshankMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

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

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("CRUIKSHANK: CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
