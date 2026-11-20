package com.acikek.qcraft.world.state.frequency;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Frequential {

    public final BlockPos pos;
    public Optional<UUID> frequency;

    public Frequential(BlockPos pos, Optional<UUID> frequency) {
        this.pos = pos.immutable();
        this.frequency = frequency;
    }

    public static Optional<UUID> getFrequency(ItemStack stack) {
        CompoundTag stackNbt = stack.getTag();
        return stackNbt != null && stackNbt.hasUUID("frequency")
                ? Optional.of(stackNbt.getUUID("frequency"))
                : Optional.empty();
    }

    public static <T extends Frequential> List<T> parse(Codec<List<T>> codec, CompoundTag nbt, String data) {
        return codec.parse(NbtOps.INSTANCE, nbt.getList(data, Tag.TAG_COMPOUND))
                .result()
                .orElse(Collections.emptyList());
    }

    public static <T extends Frequential> void encode(Codec<List<T>> codec, List<T> list, CompoundTag nbt, String data) {
        codec.encodeStart(NbtOps.INSTANCE, list)
                .result()
                .ifPresent(tag -> nbt.put(data, tag));
    }
}
