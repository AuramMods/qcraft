package com.acikek.qcraft.world.state;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.advancement.Criteria;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.world.state.frequency.Frequential;
import com.acikek.qcraft.world.state.location.QBlockLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class QBlockData extends LocationState<QBlockLocation, QBlockLocation.Pair> {

    private static final Keys KEYS = new Keys("qblock");
    private static final int INERT = QBlockLocation.INERT;
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
        QBlockLocation location = new QBlockLocation(pos, Frequential.getFrequency(stack), type, List.of(faces), QBlockItem.getFaceData(stack));
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

    public void captureFace(BlockPos pos, BlockState state, BlockEntity entity) {
        if (state.hasBlockEntity() && entity != null) {
            findDrop(pos).ifPresent(location -> {
                if (location.captureFace(state, entity)) {
                    setDirty();
                }
            });
        }
    }

    private void display(QBlockLocation location, ServerLevel level, int face) {
        if (!level.hasChunkAt(location.pos)) {
            location.pendingFace = face;
            setDirty();
            return;
        }

        BlockPos pos = location.pos;
        BlockState before = level.getBlockState(pos);
        BlockState target = location.getFaceState(face);
        BlockPos previous = transforming;
        transforming = pos;
        try {
            if (before.hasBlockEntity() || target.hasBlockEntity()) {
                displayEntityFace(location, level, face, before, target);
            } else {
                level.setBlockAndUpdate(pos, target);
                location.displayedFace = face;
            }
        } finally {
            transforming = previous;
            location.pendingFace = -1;
            setDirty();
        }
    }

    private void displayEntityFace(QBlockLocation location, ServerLevel level, int face, BlockState before, BlockState target) {
        BlockPos pos = location.pos;
        if (location.displayedFace == face && before.is(target.getBlock())) {
            return;
        }
        BlockState inert = location.getFaceState(INERT);
        try {
            captureFace(pos, before, before.hasBlockEntity() ? level.getBlockEntity(pos) : null);
        } catch (RuntimeException error) {
            QCraft.LOGGER.error("Could not save qBlock face at {}", pos, error);
            return;
        }
        try {
            if (before.hasBlockEntity() && target.hasBlockEntity()) {
                // Distinct faces of the same block still need independent block entities.
                replaceFace(level, pos, inert);
            }
            replaceFace(level, pos, target);
            if (target.hasBlockEntity()) {
                BlockEntity entity = level.getBlockEntity(pos);
                if (entity == null) {
                    throw new IllegalStateException("Missing block entity for " + target);
                }
                location.restoreFace(face, level, entity);
            }
            location.displayedFace = face;
        } catch (RuntimeException error) {
            try {
                replaceFace(level, pos, inert);
                location.displayedFace = INERT;
            } catch (RuntimeException recoveryError) {
                error.addSuppressed(recoveryError);
            }
            QCraft.LOGGER.error("Could not display qBlock face {} at {}", face, pos, error);
        }
        // Notify neighbors and clients only after the new tile has all of its data.
        level.markAndNotifyBlock(pos, level.getChunkAt(pos), before, level.getBlockState(pos), Block.UPDATE_ALL, 512);
        level.updateNeighbourForOutputSignal(pos, before.getBlock());
    }

    private static void replaceFace(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE) && level.getBlockState(pos) != state) {
            throw new IllegalStateException("Could not replace qBlock with " + state);
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
