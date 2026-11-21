package com.acikek.qcraft.block.qblock;

import com.acikek.qcraft.block.Blocks;
import com.acikek.qcraft.block.FrequentialItem;
import com.acikek.qcraft.item.Goggles;
import com.acikek.qcraft.world.state.QBlockData;
import com.acikek.qcraft.world.state.location.QBlockLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class QBlockItem extends FrequentialItem {

    private static final String FACE_DATA = "face_data";

    public QBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    public QBlock getQBlock() {
        return (QBlock) getBlock();
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (getFaces(context.getItemInHand()) == null) {
            return false;
        }
        BlockState inertState = getQBlock().type.resolveInert().defaultBlockState();
        if (!super.placeBlock(context, inertState)) {
            return false;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            QBlockData data = QBlockData.get(level);
            QBlockLocation added = data.add(getQBlock().type, context.getClickedPos(), context.getItemInHand());
            if (added != null && context.getPlayer() != null
                    && !Goggles.isWearingGoggles(context.getPlayer(), Goggles.Type.ANTI_OBSERVATION)) {
                data.pseudoObserve(added, level, context.getPlayer());
            }
        }
        return true;
    }

    public static String[] getFaces(ItemStack stack) {
        CompoundTag nbt = stack.getTagElement("faces");
        if (nbt == null) {
            return null;
        }
        String[] faces = new String[6];
        for (int i = 0; i < faces.length; i++) {
            String face = nbt.getString(QBlock.Face.values()[i].name());
            faces[i] = ResourceLocation.tryParse(face) == null || face.isEmpty() ? "minecraft:air" : face;
        }
        return faces;
    }

    public static boolean equals(ItemStack stack, ItemStack other) {
        if (stack.getItem() instanceof QBlockItem left && other.getItem() instanceof QBlockItem right) {
            return QBlock.byItem(left).type == QBlock.byItem(right).type
                    && Arrays.equals(getFaces(stack), getFaces(other))
                    && getFaceData(stack).equals(getFaceData(other));
        }
        return false;
    }

    public static void applyFaces(ItemStack stack, List<String> faces) {
        for (int i = 0; i < faces.size(); i++) {
            QBlock.Face.values()[i].apply(stack, faces.get(i));
        }
    }

    public static CompoundTag getFaceData(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(FACE_DATA);
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public static void applyFaceData(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.removeTagKey(FACE_DATA);
        } else {
            stack.addTagElement(FACE_DATA, tag.copy());
        }
    }

    public static MutableComponent formatFace(MutableComponent face, MutableComponent block) {
        block.setStyle(block.getStyle().withItalic(false)).withStyle(ChatFormatting.GRAY);
        MutableComponent faceText = Component.literal(" (").append(face).append(")")
                .setStyle(Style.EMPTY.withItalic(false))
                .withStyle(ChatFormatting.DARK_GRAY);
        block.append(faceText);
        return block;
    }

    public static ItemStack[] getPylonBases() {
        ItemStack stack = new ItemStack(Blocks.OBSERVER_DEPENDENT_BLOCK.get());
        applyFaces(stack, Collections.nCopies(6, "minecraft:obsidian"));
        ItemStack[] stacks = new ItemStack[4];
        for (int i = 0; i < stacks.length; i++) {
            ItemStack base = stack.copy();
            QBlock.Face.CARDINALS[i].apply(base, "minecraft:gold_block");
            stacks[i] = base;
        }
        return stacks;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        String[] faces = getFaces(stack);
        if (faces == null) {
            return;
        }
        for (int i = 0; i < faces.length; i++) {
            MutableComponent face = QBlock.Face.values()[i].text;
            MutableComponent block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(faces[i])).getName();
            tooltip.add(formatFace(face, block));
        }
        super.appendHoverText(stack, world, tooltip, context);
    }
}
