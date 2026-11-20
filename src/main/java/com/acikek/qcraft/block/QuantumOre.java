package com.acikek.qcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class QuantumOre extends RedStoneOreBlock {

    public static final BooleanProperty LIT = RedStoneOreBlock.LIT;

    public static Properties properties(boolean deepslate) {
        return Properties.copy(deepslate ? net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE
                        : net.minecraft.world.level.block.Blocks.REDSTONE_ORE)
                .strength(deepslate ? 4.5f : 3.0f, 5.0f)
                .lightLevel(state -> state.getValue(LIT) ? 9 : 0);
    }

    // RGB(30, 199, 106)
    public static final int CONDENSED_GREEN = 2017130;

    public static final DustParticleOptions DUST_PARTICLE = new DustParticleOptions(Vec3.fromRGB24(CONDENSED_GREEN).toVector3f(), 1.0f);

    public QuantumOre(Properties settings) {
        super(settings);
    }

    public static void spawnParticles(Level world, BlockPos pos) {
        RandomSource random = world.random;
        for (Direction direction : Direction.values()) {
            BlockPos blockPos = pos.relative(direction);
            if (!world.getBlockState(blockPos).isSolidRender(world, blockPos)) {
                Direction.Axis axis = direction.getAxis();
                double e = axis == Direction.Axis.X ? 0.5D + 0.5625D * (double) direction.getStepX() : (double) random.nextFloat();
                double f = axis == Direction.Axis.Y ? 0.5D + 0.5625D * (double) direction.getStepY() : (double) random.nextFloat();
                double g = axis == Direction.Axis.Z ? 0.5D + 0.5625D * (double) direction.getStepZ() : (double) random.nextFloat();
                world.addParticle(DUST_PARTICLE, (double) pos.getX() + e, (double) pos.getY() + f, (double) pos.getZ() + g, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    public void interact(BlockState state, Level world, BlockPos pos) {
        spawnParticles(world, pos);
        if (!state.getValue(LIT)) {
            world.setBlock(pos, state.setValue(LIT, true), 3);
        }
    }

    // Override these methods with the exact same code since RedstoneOreBlock uses private static implementations of
    // spawnParticles and light

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) {
            spawnParticles(world, pos);
        } else {
            interact(state, world, pos);
        }
        ItemStack itemStack = player.getItemInHand(hand);
        return itemStack.getItem() instanceof BlockItem && (new BlockPlaceContext(player, hand, itemStack, hit)).canPlace() ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
        interact(state, world, pos);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        interact(state, world, pos);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            spawnParticles(world, pos);
        }
    }

}
