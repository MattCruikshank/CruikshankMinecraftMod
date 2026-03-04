package com.cruikshank.mod.sound;

import com.cruikshank.mod.CruikshankMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CruikshankMod.MODID);

    public static final RegistryObject<SoundEvent> GOLDEN =
            SOUND_EVENTS.register("golden",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(CruikshankMod.MODID, "golden")));

    public static final RegistryObject<SoundEvent> WOLF_TAMED =
            SOUND_EVENTS.register("entity.wolf.tamed",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(CruikshankMod.MODID, "entity.wolf.tamed")));

    public static final RegistryObject<SoundEvent> WOLF_FEED =
            SOUND_EVENTS.register("entity.wolf.feed",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(CruikshankMod.MODID, "entity.wolf.feed")));

    public static final RegistryObject<SoundEvent> WOLF_TOLD_TO_SIT =
            SOUND_EVENTS.register("entity.wolf.told_to_sit",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(CruikshankMod.MODID, "entity.wolf.told_to_sit")));

    public static final RegistryObject<SoundEvent> WOLF_NEARBY_PLAYER_LEVEL_UP =
            SOUND_EVENTS.register("entity.wolf.nearby_player_level_up",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(CruikshankMod.MODID, "entity.wolf.nearby_player_level_up")));

    public static final RegistryObject<SoundEvent> WOLF_NEARBY_PLAYER_SLEEP =
            SOUND_EVENTS.register("entity.wolf.nearby_player_sleep",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(CruikshankMod.MODID, "entity.wolf.nearby_player_sleep")));
}
