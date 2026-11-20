package com.acikek.qcraft.block;

import com.acikek.qcraft.block.qblock.QBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FrequentialItem extends BlockItem {

    public FrequentialItem(Block block, Properties settings) {
        super(block, settings);
    }

    public static boolean isUnavailable(ItemStack stack) {
        return !(stack.getItem() instanceof FrequentialItem)
                || stack.getTag() != null && stack.getTag().hasUUID("frequency");
    }

    public static boolean checkStacks(ItemStack left, ItemStack right, boolean qBlock) {
        if (isUnavailable(left) || isUnavailable(right)) {
            return false;
        }
        if (qBlock) {
            return QBlockItem.equals(left, right);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        if (stack.getTag() != null && stack.getTag().hasUUID("frequency")) {
            String uuid = stack.getTag().getUUID("frequency").toString();
            String snippet = uuid.substring(0, uuid.indexOf('-')) + "...";
            MutableComponent frequency = Component.translatable("tooltip.qcraft.frequency")
                    .append(": ")
                    .setStyle(Style.EMPTY.withItalic(false))
                    .withStyle(ChatFormatting.GRAY);
            Component text = Component.literal(snippet)
                    .withStyle(ChatFormatting.DARK_AQUA);
            frequency.append(text);
            tooltip.add(frequency);
        }
    }
}
