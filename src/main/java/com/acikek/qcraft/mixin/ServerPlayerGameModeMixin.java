package com.acikek.qcraft.mixin;

import com.acikek.qcraft.world.state.QBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "removeBlock", at = @At("HEAD"), remap = false)
    private void qcraft$saveTileBeforeRemoval(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> result) {
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity()) {
            QBlockData data = QBlockData.get(level);
            if (data.locations.has(pos)) {
                data.captureFace(pos, state, level.getBlockEntity(pos));
            }
        }
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void qcraft$harvestStoredTile(Block block, Level world, Player player, BlockPos pos, BlockState state, BlockEntity entity, ItemStack tool) {
        if (entity != null) {
            var drop = QBlockData.get(level).findDrop(pos);
            if (drop.isPresent()) {
                drop.get().type.resolveInert().playerDestroy(world, player, pos, state, null, tool);
                return;
            }
        }
        block.playerDestroy(world, player, pos, state, entity, tool);
    }
}
