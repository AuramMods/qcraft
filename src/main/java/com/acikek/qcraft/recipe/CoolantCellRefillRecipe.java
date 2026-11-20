package com.acikek.qcraft.recipe;

import com.acikek.qcraft.item.Items;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CoolantCellRefillRecipe extends CustomRecipe {

    public CoolantCellRefillRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess manager) {
        return new ItemStack(Items.COOLANT_CELL.get());
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        if (inventory.countItem(net.minecraft.world.item.Items.BLUE_ICE) != 1 || inventory.countItem(Items.COOLANT_CELL.get()) != 1) {
            return false;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.COOLANT_CELL.get())) {
                if (stack.getDamageValue() == 0) {
                    return false;
                }
            }
            else if (!stack.isEmpty() && !stack.is(net.minecraft.world.item.Items.BLUE_ICE)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.COOLANT_CELL_REFILL.get();
    }
}
