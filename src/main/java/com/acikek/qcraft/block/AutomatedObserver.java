package com.acikek.qcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class AutomatedObserver extends DiodeBlock {

    public static final Properties DEFAULT_SETTINGS = Properties.of().noOcclusion()
            .instabreak()
            .sound(SoundType.WOOD);

    public AutomatedObserver() {
        super(DEFAULT_SETTINGS);
        registerDefaultState(
                getStateDefinition().any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(POWERED, false)
        );
    }

    @Override
    protected boolean shouldTurnOn(Level world, BlockPos pos, BlockState state) {
        return getInputSignal(world, pos, state) > 0;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) {
            return;
        }
        Direction direction = state.getValue(FACING);
        double d = (double) pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double e = (double) pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
        double f = (double) pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        float g = -5.0f;
        double h = (g /= 16.0f) * (float) direction.getStepX();
        double i = g * (float) direction.getStepZ();
        world.addParticle(QuantumOre.DUST_PARTICLE, d + h, e, f + i, 0.0, 0.0, 0.0);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected int getDelay(BlockState state) {
        return 0;
    }
}
