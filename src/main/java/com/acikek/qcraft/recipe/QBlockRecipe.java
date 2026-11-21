package com.acikek.qcraft.recipe;

import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.item.QBlockEssence;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class QBlockRecipe extends CustomRecipe {

    public QBlockRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess manager) {
        QBlockEssence essence = (QBlockEssence) inventory.getItem(QBlock.Face.CENTER).getItem();
        List<String> faces = new ArrayList<>();
        CompoundTag faceData = new CompoundTag();
        for (QBlock.Face face : QBlock.Face.values()) {
            ItemStack ingredient = inventory.getItem(face.slot);
            if (ingredient.getItem() instanceof BlockItem blockItem) {
                var block = blockItem.getBlock();
                faces.add(BuiltInRegistries.BLOCK.getKey(block).toString());
                if (block.defaultBlockState().hasBlockEntity()) {
                    CompoundTag item = ingredient.copyWithCount(1).save(new CompoundTag());
                    if (!ingredient.is(block.asItem()) || item.contains("tag", Tag.TAG_COMPOUND)
                            || item.contains("ForgeCaps", Tag.TAG_COMPOUND)) {
                        CompoundTag seed = new CompoundTag();
                        seed.put("item", item);
                        faceData.put(face.name(), seed);
                    }
                }
            } else {
                faces.add("minecraft:air");
            }
        }
        ItemStack stack = new ItemStack(essence.getQBlock());
        QBlockItem.applyFaces(stack, faces);
        QBlockItem.applyFaceData(stack, faceData);
        return stack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        if (!canCraftInDimensions(inventory.getWidth(), inventory.getHeight())
                || !(inventory.getItem(QBlock.Face.CENTER).getItem() instanceof QBlockEssence)) {
            return false;
        }
        for (int i : QBlock.Face.EMPTY_SLOTS) {
            if (!inventory.getItem(i).isEmpty()) {
                return false;
            }
        }
        ItemStack first = inventory.getItem(QBlock.Face.UP.slot);
        boolean oneDifferent = false;
        for (QBlock.Face face : QBlock.Face.values()) {
            ItemStack stack = inventory.getItem(face.slot);
            var item = stack.getItem();
            if (!stack.isEmpty() && (!(item instanceof BlockItem) || item instanceof QBlockItem)) {
                return false;
            }
            if (item != first.getItem()) {
                oneDifferent = true;
            }
        }
        return oneDifferent;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.Q_BLOCK.get();
    }
}
