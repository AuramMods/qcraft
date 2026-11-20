package com.acikek.qcraft.recipe;

import com.acikek.qcraft.block.FrequentialItem;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.item.Essence;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class EntangledPairRecipe extends CustomRecipe {

    public EntangledPairRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    public static void applyFrequency(ItemStack stack, UUID uuid) {
        stack.getOrCreateTag().putUUID("frequency", uuid);
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess manager) {
        ItemStack stack = inventory.getItem(Essence.findSlot(inventory) - 1).copy();
        applyFrequency(stack, UUID.randomUUID());
        stack.setCount(2);
        return stack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3;
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        int essenceSlot = Essence.findSlot(inventory);
        int width = inventory.getWidth();
        if (width < 3 || essenceSlot < 0 || essenceSlot % width == 0 || essenceSlot % width == width - 1
                || ((Essence) inventory.getItem(essenceSlot).getItem()).essenceType != Essence.Type.ENTANGLEMENT) {
            return false;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if ((i < essenceSlot - 1 || i > essenceSlot + 1) && !inventory.getItem(i).isEmpty()) {
                return false;
            }
        }
        ItemStack left = inventory.getItem(essenceSlot - 1);
        ItemStack right = inventory.getItem(essenceSlot + 1);
        if (left.getItem() != right.getItem()) {
            return false;
        }
        return FrequentialItem.checkStacks(left, right, left.getItem() instanceof QBlockItem);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.ENTANGLED_PAIR.get();
    }
}
