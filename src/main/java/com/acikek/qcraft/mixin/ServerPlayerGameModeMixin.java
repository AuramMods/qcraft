package com.acikek.qcraft.mixin;

import com.acikek.qcraft.world.state.QBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void qcraft$beforePlayerBreak(BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        QBlockData.get(level).beginPlayerBreak(pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void qcraft$afterPlayerBreak(BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        QBlockData.get(level).endPlayerBreak();
    }
}
