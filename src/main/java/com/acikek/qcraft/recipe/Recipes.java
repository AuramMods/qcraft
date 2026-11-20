package com.acikek.qcraft.recipe;

import com.acikek.qcraft.QCraft;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Recipes {

    public static final DeferredRegister<RecipeSerializer<?>> REGISTER = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, QCraft.ID);
    public static final RegistryObject<RecipeSerializer<?>> Q_BLOCK = REGISTER.register("crafting_special_qblock",
            () -> new SimpleCraftingRecipeSerializer<>(QBlockRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> ENTANGLED_PAIR = REGISTER.register("crafting_special_entangled_pair",
            () -> new SimpleCraftingRecipeSerializer<>(EntangledPairRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> COOLANT_CELL_REFILL = REGISTER.register("crafting_special_coolant_cell_refill",
            () -> new SimpleCraftingRecipeSerializer<>(CoolantCellRefillRecipe::new));
}
