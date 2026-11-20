package com.acikek.qcraft;

import com.acikek.qcraft.advancement.Criteria;
import com.acikek.qcraft.block.Blocks;
import com.acikek.qcraft.block.quantum_computer.QuantumComputerBlockEntity;
import com.acikek.qcraft.block.quantum_computer.QuantumComputerMenu;
import com.acikek.qcraft.command.QCraftCommand;
import com.acikek.qcraft.item.Items;
import com.acikek.qcraft.recipe.Recipes;
import com.acikek.qcraft.sound.Sounds;
import com.acikek.qcraft.world.QBlockTickListener;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(QCraft.ID)
public class QCraft {

    public static final String ID = "qcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.qcraft.main"))
                .icon(() -> new ItemStack(Items.QUANTUM_DUST.get()))
                .displayItems((parameters, output) ->
                        Items.REGISTER.getEntries().forEach(item -> output.accept(item.get())))
                .build());
    }

    public QCraft(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        Blocks.REGISTER.register(bus);
        Items.REGISTER.register(bus);
        Sounds.REGISTER.register(bus);
        Recipes.REGISTER.register(bus);
        QuantumComputerBlockEntity.REGISTER.register(bus);
        QuantumComputerMenu.REGISTER.register(bus);
        TABS.register(bus);
        bus.addListener(Criteria::register);
        MinecraftForge.EVENT_BUS.addListener(QCraftCommand::register);
        MinecraftForge.EVENT_BUS.addListener(QBlockTickListener::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(QBlockTickListener::onPistonMove);
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.of(ID + ':' + name, ':');
    }

    public static String uid(String item) {
        return ID + "_" + item;
    }
}
