package com.acikek.qcraft.sound;

import com.acikek.qcraft.QCraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Sounds {

    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, QCraft.ID);
    public static final SoundEvent QUANTUM_COMPUTER_PLACE = register("block.quantum_computer.place");
    public static final SoundEvent QUANTUM_COMPUTER_BREAK = register("block.quantum_computer.break");

    private static SoundEvent register(String name) {
        SoundEvent sound = SoundEvent.createVariableRangeEvent(QCraft.id(name));
        REGISTER.register(name, () -> sound);
        return sound;
    }
}
