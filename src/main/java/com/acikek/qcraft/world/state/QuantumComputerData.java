package com.acikek.qcraft.world.state;

import com.acikek.qcraft.world.state.frequency.Frequential;
import com.acikek.qcraft.world.state.location.QuantumComputerLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class QuantumComputerData extends LocationState<QuantumComputerLocation, QuantumComputerLocation.Pair> {

    private static final Keys KEYS = new Keys("qc");

    /**
     * Gets or creates {@link QuantumComputerData} state from the specified world.
     *
     * @param world The {@link ServerLevel} to get the state from.
     * @return The {@link QuantumComputerData} instance.
     */
    public static QuantumComputerData get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(QuantumComputerData::fromNbt, QuantumComputerData::new, KEYS.id());
    }

    public static QuantumComputerData fromNbt(CompoundTag nbt) {
        QuantumComputerData data = new QuantumComputerData();
        List<QuantumComputerLocation> locations = Frequential.parse(QuantumComputerLocation.LIST_CODEC, nbt, KEYS.data());
        if (!locations.isEmpty()) {
            data.fill(locations, QuantumComputerLocation.Pair::new);
        }
        return data;
    }

    /**
     * Constructs and adds a {@link QuantumComputerLocation} to this state's locations.
     *
     * @return The added location.
     */
    public QuantumComputerLocation add(BlockPos blockPos, ItemStack stack) {
        if (locations.has(blockPos)) {
            return null;
        }
        Optional<UUID> frequency = Frequential.getFrequency(stack);
        QuantumComputerLocation result = new QuantumComputerLocation(blockPos, frequency);
        locations.add(result);
        frequency.ifPresent(f -> frequencies.add(f, result, QuantumComputerLocation.Pair::new));
        setDirty();
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        Frequential.encode(QuantumComputerLocation.LIST_CODEC, List.copyOf(locations.values()), nbt, KEYS.data());
        return nbt;
    }
}
