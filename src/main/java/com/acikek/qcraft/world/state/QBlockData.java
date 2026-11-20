package com.acikek.qcraft.world.state;

import com.acikek.qcraft.advancement.Criteria;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.world.state.frequency.Frequential;
import com.acikek.qcraft.world.state.location.QBlockLocation;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class QBlockData extends LocationState<QBlockLocation, QBlockLocation.Pair> {

    private static final Keys KEYS = new Keys("qblock");
    private static final int INERT = QBlock.Face.values().length;
    private final Deque<Optional<QBlockLocation>> playerBreaks = new ArrayDeque<>();
    private BlockPos transforming;

    public static QBlockData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(QBlockData::fromNbt, QBlockData::new, KEYS.id());
    }

    public static QBlockData fromNbt(CompoundTag tag) {
        QBlockData data = new QBlockData();
        data.fill(Frequential.parse(QBlockLocation.LIST_CODEC, tag, KEYS.data()), QBlockLocation.Pair::new);
        return data;
    }

    public List<QBlockLocation> getLoadedLocations(ServerLevel level) {
        return locations.values().stream().filter(location -> level.hasChunkAt(location.pos)).toList();
    }

    public QBlockLocation add(QBlock.Type type, BlockPos pos, ItemStack stack) {
        String[] faces = QBlockItem.getFaces(stack);
        if (faces == null) {
            return null;
        }
        QBlockLocation location = new QBlockLocation(pos, Frequential.getFrequency(stack), type, List.of(faces), false);
        if (!locations.add(location)) {
            return null;
        }
        location.frequency.ifPresent(id -> frequencies.add(id, location, QBlockLocation.Pair::new));
        setDirty();
        return location;
    }

    public void onBlockReplaced(ServerLevel level, BlockPos pos) {
        if (!pos.equals(transforming)) {
            locations.get(pos).ifPresent(location -> {
                QBlockLocation other = frequencies.counterpart(location);
                remove(location);
                if (other != null && !other.observed) {
                    display(other, level, INERT);
                }
            });
        }
    }

    public void beginPlayerBreak(BlockPos pos) {
        playerBreaks.push(locations.get(pos));
    }

    public void endPlayerBreak() {
        playerBreaks.pop();
    }

    public Optional<QBlockLocation> findDrop(BlockPos pos) {
        Optional<QBlockLocation> live = locations.get(pos);
        if (live.isPresent() || playerBreaks.isEmpty()) {
            return live;
        }
        return playerBreaks.peek().filter(location -> location.pos.equals(pos));
    }

    private void display(QBlockLocation location, ServerLevel level, int face) {
        if (!level.hasChunkAt(location.pos)) {
            location.pendingFace = face;
            setDirty();
            return;
        }

        BlockState state = face == INERT
                ? location.type.resolveInert().defaultBlockState()
                : location.getFaceBlock(face).defaultBlockState();
        BlockPos previous = transforming;
        transforming = location.pos;
        try {
            level.setBlockAndUpdate(location.pos, state);
            location.pendingFace = -1;
            setDirty();
        } finally {
            transforming = previous;
        }
    }

    public void applyPendingDisplay(QBlockLocation location, ServerLevel level) {
        if (location.pendingFace >= 0) {
            display(location, level, location.pendingFace);
        }
    }

    public void pseudoObserve(QBlockLocation location, ServerLevel level, Player player) {
        display(location, level, location.pickFace(player, level).index);
    }

    public void observe(QBlockLocation location, ServerLevel level, Player player) {
        observe(location, level, location.pickFace(player, level), QBlock.Observation.PLAYER, player);
    }

    public void observe(QBlockLocation location, ServerLevel level, QBlock.Face face, QBlock.Observation type, Player player) {
        location.observed = true;
        display(location, level, face.index);
        trigger(player, location, type, false);
        QBlockLocation other = frequencies.counterpart(location);
        if (other != null) {
            display(other, level, face.index);
            trigger(player, other, type, true);
        }
    }

    private static void trigger(Player player, QBlockLocation location, QBlock.Observation type, boolean paired) {
        if (player instanceof ServerPlayer serverPlayer) {
            Criteria.QUANTUM_OBSERVATION.trigger(serverPlayer, location.pos, type, location.type, paired);
        }
    }

    public void unobserve(QBlockLocation location, ServerLevel level) {
        location.observed = false;
        display(location, level, INERT);
        QBlockLocation other = frequencies.counterpart(location);
        if (other != null && !other.observed) {
            display(other, level, INERT);
        }
    }

    public boolean isUnobserved(QBlockLocation location) {
        QBlockLocation other = frequencies.counterpart(location);
        return !location.observed && (other == null || !other.observed);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        Frequential.encode(QBlockLocation.LIST_CODEC, List.copyOf(locations.values()), tag, KEYS.data());
        return tag;
    }
}
