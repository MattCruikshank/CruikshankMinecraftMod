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
}
