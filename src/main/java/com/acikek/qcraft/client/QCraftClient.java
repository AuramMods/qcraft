package com.acikek.qcraft.client;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.quantum_computer.QuantumComputerMenu;
import com.acikek.qcraft.client.gui.screen.QuantumComputerScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = QCraft.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class QCraftClient {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(QuantumComputerMenu.TYPE.get(), QuantumComputerScreen::new));
    }
}
