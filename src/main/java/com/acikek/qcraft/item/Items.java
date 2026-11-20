package com.acikek.qcraft.item;

import com.acikek.qcraft.QCraft;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Items {

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, QCraft.ID);
    public static final RegistryObject<Item> QUANTUM_DUST = REGISTER.register("quantum_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COOLANT_CELL = REGISTER.register("coolant_cell", () -> new Item(new Item.Properties().durability(100)));
    public static final RegistryObject<QBlockEssence> OBSERVATION_ESSENCE = REGISTER.register("observation_essence",
            () -> new QBlockEssence(new Item.Properties(), Essence.Type.OBSERVATION));
    public static final RegistryObject<QBlockEssence> SUPERPOSITION_ESSENCE = REGISTER.register("superposition_essence",
            () -> new QBlockEssence(new Item.Properties(), Essence.Type.SUPERPOSITION));
    public static final RegistryObject<Essence> ENTANGLEMENT_ESSENCE = REGISTER.register("entanglement_essence",
            () -> new Essence(new Item.Properties(), Essence.Type.ENTANGLEMENT));
    public static final RegistryObject<Goggles> QUANTUM_GOGGLES = REGISTER.register("quantum_goggles", () -> new Goggles(Goggles.Type.QUANTUM));
    public static final RegistryObject<Goggles> ANTI_OBSERVATION_GOGGLES = REGISTER.register("anti_observation_goggles", () -> new Goggles(Goggles.Type.ANTI_OBSERVATION));
}
