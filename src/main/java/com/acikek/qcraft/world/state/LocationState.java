package com.acikek.qcraft.world.state;

import com.acikek.qcraft.world.state.frequency.FrequencyMap;
import com.acikek.qcraft.world.state.frequency.Frequential;
import com.acikek.qcraft.world.state.frequency.LocationList;
import com.acikek.qcraft.world.state.frequency.Pair;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class LocationState<T extends Frequential, P extends Pair<T>> extends SavedData {

    public final LocationList<T> locations = new LocationList<>();
    public final FrequencyMap<T, P> frequencies = new FrequencyMap<>();

    public int clearLocations() {
        int size = locations.clear();
        frequencies.map.clear();
        setDirty();
        return size;
    }

    public int clearFrequencies() {
        int size = frequencies.map.size();
        frequencies.map.clear();
        locations.values().forEach(location -> location.frequency = Optional.empty());
        setDirty();
        return size;
    }

    public int reset() {
        return clearFrequencies() + clearLocations();
    }

    public void fill(List<T> saved, Function<T, P> createPair) {
        for (T location : saved) {
            if (locations.add(location)) {
                location.frequency.ifPresent(frequency -> frequencies.add(frequency, location, createPair));
            }
        }
    }

    public void remove(T location) {
        if (locations.remove(location)) {
            frequencies.remove(location);
            setDirty();
        }
    }

    public record Keys(String id, String data) {
        public Keys(String item) {
            this("qcraft_" + item + "_data", "qcraft_" + item + "_locations");
        }
    }
}
