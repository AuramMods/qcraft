package com.acikek.qcraft.advancement;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class Criteria {

    public static final QuantumObservationCriterion QUANTUM_OBSERVATION = new QuantumObservationCriterion();
    public static final QuantumTeleportationCriterion QUANTUM_TELEPORTATION = new QuantumTeleportationCriterion();

    public static void register(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CriteriaTriggers.register(QUANTUM_OBSERVATION);
            CriteriaTriggers.register(QUANTUM_TELEPORTATION);
        });
    }
}
