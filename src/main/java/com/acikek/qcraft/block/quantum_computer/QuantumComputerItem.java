package com.acikek.qcraft.block.quantum_computer;

import com.acikek.qcraft.block.FrequentialItem;
import com.acikek.qcraft.world.state.QuantumComputerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class QuantumComputerItem extends FrequentialItem {

    public QuantumComputerItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (super.placeBlock(context, state)) {
            if (context.getLevel() instanceof ServerLevel level
                    && level.getBlockEntity(context.getClickedPos()) instanceof QuantumComputerBlockEntity computer) {
                var location = QuantumComputerData.get(level).add(context.getClickedPos(), context.getItemInHand());
                if (location != null) {
                    computer.setFrequency(location.frequency);
                }
            }
            return true;
        }
        return false;
    }
}
