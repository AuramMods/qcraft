package com.acikek.qcraft.world.state.frequency;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class FrequencyMap<T extends Frequential, P extends Pair<T>> {

    public final Map<UUID, P> map = new HashMap<>();

    public T counterpart(T item) {
        P pair = item.frequency.map(map::get).orElse(null);
        return pair == null ? null : pair.getOther(item);
    }

    public void add(UUID id, T item, Function<T, P> createPair) {
        P pair = map.get(id);
        if (pair == null) {
            map.put(id, createPair.apply(item));
        } else {
            pair.add(item);
        }
    }

    public void remove(T item) {
        item.frequency.ifPresent(id -> {
            P pair = map.get(id);
            if (pair != null && pair.remove(item)) {
                map.remove(id);
            }
        });
    }
}
