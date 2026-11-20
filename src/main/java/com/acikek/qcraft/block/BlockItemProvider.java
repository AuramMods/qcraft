package com.acikek.qcraft.block;

import java.util.function.BiFunction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public interface BlockItemProvider {
    BiFunction<Block, Item.Properties, BlockItem> getBlockItem();
}
