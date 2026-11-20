package com.acikek.qcraft.world.state.frequency;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class LocationList<T extends Frequential> {

    private final Map<BlockPos, T> locations = new LinkedHashMap<>();

    public Optional<T> get(BlockPos pos) {
        return Optional.ofNullable(locations.get(pos));
    }

    public boolean has(BlockPos pos) {
        return locations.containsKey(pos);
    }

    public boolean add(T location) {
        return locations.putIfAbsent(location.pos, location) == null;
    }

    public boolean remove(T location) {
        return locations.remove(location.pos, location);
    }

    public Collection<T> values() {
        return locations.values();
    }

    public int clear() {
        int size = locations.size();
        locations.clear();
        return size;
    }
}
