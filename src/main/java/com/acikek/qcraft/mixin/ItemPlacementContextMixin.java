package com.acikek.qcraft.mixin;

import com.acikek.qcraft.world.state.QBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlaceContext.class)
public abstract class ItemPlacementContextMixin extends UseOnContext {

    public ItemPlacementContextMixin(Player player, InteractionHand hand, BlockHitResult hit) {
        super(player, hand, hit);
    }

    @Shadow public abstract BlockPos getClickedPos();

    @Inject(method = "canPlace", cancellable = true, at = @At(value = "HEAD"))
    private void cancelIfQBlock(CallbackInfoReturnable<Boolean> cir) {
        if (getLevel() instanceof ServerLevel level && QBlockData.get(level).locations.has(getClickedPos())) {
            cir.setReturnValue(false);
        }
    }
}
