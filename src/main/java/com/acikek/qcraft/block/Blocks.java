package com.acikek.qcraft.block;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.qblock.InertQBlock;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.block.quantum_computer.QuantumComputer;
import com.acikek.qcraft.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class Blocks {

    public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(ForgeRegistries.BLOCKS, QCraft.ID);
    public static final RegistryObject<Block> QUANTUM_ORE = register("quantum_ore", () -> new QuantumOre(QuantumOre.properties(false)));
    public static final RegistryObject<Block> DEEPSLATE_QUANTUM_ORE = register("deepslate_quantum_ore", () -> new QuantumOre(QuantumOre.properties(true)));
    public static final RegistryObject<Block> QUANTUM_DUST_BLOCK = register("quantum_dust_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).lightLevel(state -> 1).strength(1.0f)));
    public static final RegistryObject<InertQBlock> INERT_OBSERVER_DEPENDENT_BLOCK = register("inert_observer_dependent_block", InertQBlock::new);
    public static final RegistryObject<InertQBlock> INERT_QUANTUM_BLOCK = register("inert_quantum_block", InertQBlock::new);
    public static final RegistryObject<QBlock> OBSERVER_DEPENDENT_BLOCK = register("observer_dependent_block", () -> new QBlock(QBlock.Type.OBSERVER_DEPENDENT));
    public static final RegistryObject<QBlock> QUANTUM_BLOCK = register("quantum_block", () -> new QBlock(QBlock.Type.QUANTUM));
    public static final RegistryObject<AutomatedObserver> AUTOMATED_OBSERVER = register("automated_observer", AutomatedObserver::new);
    public static final RegistryObject<QuantumComputer> QUANTUM_COMPUTER = register("quantum_computer", QuantumComputer::new);

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> supplier) {
        RegistryObject<T> block = REGISTER.register(name, supplier);
        Items.REGISTER.register(name, () -> {
            T value = block.get();
            Item.Properties properties = new Item.Properties();
            return value instanceof BlockItemProvider provider
                    ? provider.getBlockItem().apply(value, properties)
                    : new BlockItem(value, properties);
        });
        return block;
    }
}
