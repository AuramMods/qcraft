package com.acikek.qcraft.world.state.location;

import com.acikek.qcraft.block.Blocks;
import com.acikek.qcraft.world.state.frequency.Frequential;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

public class QuantumComputerLocation extends Frequential {

    public static final Codec<QuantumComputerLocation> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(l -> l.pos),
                    UUIDUtil.CODEC.optionalFieldOf("frequency").forGetter(l -> l.frequency)
            )
            .apply(instance, QuantumComputerLocation::new)
    );

    public static final Codec<List<QuantumComputerLocation>> LIST_CODEC = Codec.list(CODEC);

    public QuantumComputerLocation(BlockPos pos, Optional<UUID> frequency) {
        super(pos, frequency);
    }

    /**
     * @return The stack with frequency applied, if any.
     */
    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(Blocks.QUANTUM_COMPUTER.get());
        frequency.ifPresent(f -> stack.getOrCreateTag().putUUID("frequency", f));
        return stack;
    }

    public static class Pair extends com.acikek.qcraft.world.state.frequency.Pair<QuantumComputerLocation> {

        public Pair(QuantumComputerLocation first) {
            super(first);
        }
    }
}
