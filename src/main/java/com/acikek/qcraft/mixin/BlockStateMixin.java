package com.acikek.qcraft.mixin;

import com.acikek.qcraft.world.state.QBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateMixin {

    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
    private void qcraft$dropQBlock(LootParams.Builder loot, CallbackInfoReturnable<List<ItemStack>> result) {
        Vec3 origin = loot.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) {
            return;
        }
        QBlockData.get(loot.getLevel()).findDrop(BlockPos.containing(origin)).ifPresent(location -> {
            Float radius = loot.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
            boolean survives = radius == null || loot.getLevel().random.nextFloat() <= 1.0f / radius;
            result.setReturnValue(survives ? List.of(location.getItemStack()) : List.of());
        });
    }
}
