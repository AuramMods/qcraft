package com.acikek.qcraft.mixin;

import com.acikek.qcraft.world.state.QBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow public abstract Level getLevel();

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void qcraft$onBlockChanged(BlockPos pos, BlockState replacement, boolean moving, CallbackInfoReturnable<BlockState> result) {
        BlockState previous = result.getReturnValue();
        if (previous != null && !previous.is(replacement.getBlock()) && getLevel() instanceof ServerLevel level) {
            QBlockData.get(level).onBlockReplaced(level, pos);
        }
    }
}
