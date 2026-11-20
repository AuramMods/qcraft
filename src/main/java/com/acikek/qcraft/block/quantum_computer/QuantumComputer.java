package com.acikek.qcraft.block.quantum_computer;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.advancement.Criteria;
import com.acikek.qcraft.block.BlockItemProvider;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.item.Items;
import com.acikek.qcraft.sound.Sounds;
import com.acikek.qcraft.world.state.QBlockData;
import com.acikek.qcraft.world.state.QuantumComputerData;
import com.acikek.qcraft.world.state.location.QuantumComputerLocation;
import net.minecraft.core.BlockPos;
import com.acikek.qcraft.block.QuantumOre;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public class QuantumComputer extends Block implements BlockItemProvider, EntityBlock {

    private static final int MAX_PYLON_DISTANCE = 8;
    private static final int SWAP_FLAGS = UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS;
    private static final SoundType SOUND = new SoundType(1.0f, 1.0f, Sounds.QUANTUM_COMPUTER_BREAK,
            SoundEvents.METAL_STEP, Sounds.QUANTUM_COMPUTER_PLACE, SoundEvents.METAL_HIT, SoundEvents.METAL_FALL);

    public QuantumComputer() {
        super(Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SOUND).pushReaction(PushReaction.BLOCK));
    }

    public enum Status {
        READY("success.quantum_computer"),
        MISSING_FREQUENCY("error.missing_frequency"),
        MISSING_PYLONS("error.missing_pylons"),
        MISALIGNED("error.misaligned"),
        PYLON_DISTANCES("error.pylon_distances"),
        PYLON_HEIGHTS("error.pylon_heights"),
        MISSING_COUNTERPART("error.missing_counterpart"),
        ERRORED_COUNTERPART("error.errored_counterpart"),
        UNLOADED("error.unloaded"),
        OVERLAP("error.overlap"),
        UNSUPPORTED_BLOCKS("error.unsupported_blocks"),
        NO_COOLANT("error.no_coolant"),
        SWAP_FAILED("error.swap_failed"),
        OUT_OF_BOUNDS("error.out_of_bounds");

        private final String key;

        Status(String key) {
            this.key = "gui.qcraft." + key;
        }

        public Component text() {
            return Component.translatable(key);
        }
    }

    private record Layout(int[] offsets, int height) {
        BoundingBox region(BlockPos center) {
            return new BoundingBox(center.getX() - offsets[3] + 1, center.getY(), center.getZ() - offsets[0] + 1,
                    center.getX() + offsets[2] - 1, center.getY() + height - 1, center.getZ() + offsets[1] - 1);
        }
    }

    private record Pylons(Status status, Layout layout) {}

    private record Connection(Status status, BlockPos other, BoundingBox source, BoundingBox target) {
        Connection(Status status) {
            this(status, null, null, null);
        }
    }

    private record Change(BlockPos pos, BlockState before, BlockState after) {}

    private static Pylons findPylons(ServerLevel level, BlockPos center) {
        int[] offsets = new int[4];
        int height = 0;
        QBlockData data = QBlockData.get(level);
        for (int i = 0; i < offsets.length; i++) {
            QBlock.Face face = QBlock.Face.CARDINALS[i];
            BlockPos base = null;
            for (int distance = 1; distance <= MAX_PYLON_DISTANCE; distance++) {
                BlockPos candidate = center.relative(face.direction, distance);
                var location = data.locations.get(candidate).orElse(null);
                if (location != null && location.isPylonBase(face.getOpposite())) {
                    if (!level.hasChunkAt(candidate)) {
                        return new Pylons(Status.UNLOADED, null);
                    }
                    if (!location.isStateImpossible(level.getBlockState(candidate))) {
                        base = candidate;
                        offsets[i] = distance;
                    }
                }
            }
            if (base == null) {
                return new Pylons(Status.MISSING_PYLONS, null);
            }
            int pylonHeight = 1;
            while (base.getY() + pylonHeight < level.getMaxBuildHeight()
                    && level.getBlockState(base.above(pylonHeight)).is(Blocks.OBSIDIAN)) {
                pylonHeight++;
            }
            if (height != 0 && height != pylonHeight) {
                return new Pylons(Status.MISALIGNED, null);
            }
            height = pylonHeight;
        }
        return new Pylons(Status.READY, new Layout(offsets, height));
    }

    private static Connection findConnection(ServerLevel level, BlockPos pos) {
        QuantumComputerData data = QuantumComputerData.get(level);
        QuantumComputerLocation here = data.locations.get(pos).orElse(null);
        if (here == null || here.frequency.isEmpty()) {
            return new Connection(Status.MISSING_FREQUENCY);
        }
        QuantumComputerLocation other = data.frequencies.counterpart(here);
        if (other == null) {
            return new Connection(Status.MISSING_COUNTERPART);
        }
        if (!level.hasChunkAt(other.pos)) {
            return new Connection(Status.UNLOADED);
        }
        if (!(level.getBlockEntity(other.pos) instanceof QuantumComputerBlockEntity)) {
            return new Connection(Status.MISSING_COUNTERPART);
        }

        Pylons source = findPylons(level, pos);
        if (source.status != Status.READY) {
            return new Connection(source.status);
        }
        Pylons target = findPylons(level, other.pos);
        if (target.status != Status.READY) {
            return new Connection(target.status == Status.UNLOADED ? Status.UNLOADED : Status.ERRORED_COUNTERPART);
        }
        if (!Arrays.equals(source.layout.offsets, target.layout.offsets)) {
            return new Connection(Status.PYLON_DISTANCES);
        }
        if (source.layout.height != target.layout.height) {
            return new Connection(Status.PYLON_HEIGHTS);
        }
        BoundingBox a = source.layout.region(pos);
        BoundingBox b = target.layout.region(other.pos);
        if (!isInBounds(level, a) || !isInBounds(level, b)) {
            return new Connection(Status.OUT_OF_BOUNDS);
        }
        if (a.intersects(b)) {
            return new Connection(Status.OVERLAP);
        }
        if (!isLoaded(level, a) || !isLoaded(level, b)) {
            return new Connection(Status.UNLOADED);
        }
        return new Connection(Status.READY, other.pos, a, b);
    }

    private static boolean isInBounds(ServerLevel level, BoundingBox box) {
        BlockPos min = new BlockPos(box.minX(), box.minY(), box.minZ());
        BlockPos max = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
        var border = level.getWorldBorder();
        return level.isInWorldBounds(min) && level.isInWorldBounds(max) && border.isWithinBounds(min) && border.isWithinBounds(max);
    }

    private static boolean isLoaded(ServerLevel level, BoundingBox box) {
        return level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ());
    }

    static Status status(ServerLevel level, BlockPos pos) {
        return findConnection(level, pos).status;
    }

    static Status energize(ServerLevel level, QuantumComputerBlockEntity computer, ServerPlayer player) {
        BlockPos pos = computer.getBlockPos();
        Connection connection = findConnection(level, pos);
        if (connection.status != Status.READY) {
            return connection.status;
        }
        ItemStack coolant = computer.getItem(0);
        if (!coolant.is(Items.COOLANT_CELL.get()) || coolant.getDamageValue() >= coolant.getMaxDamage()) {
            return Status.NO_COOLANT;
        }

        QBlockData qBlocks = QBlockData.get(level);
        List<Change> changes = new ArrayList<>();
        BoundingBox a = connection.source;
        BlockPos displacement = connection.other.subtract(pos);
        for (BlockPos offset : BlockPos.betweenClosed(a.minX(), a.minY(), a.minZ(), a.maxX(), a.maxY(), a.maxZ())) {
            if (offset.equals(pos)) {
                continue;
            }
            BlockPos source = offset.immutable();
            BlockPos target = source.offset(displacement);
            BlockState sourceState = level.getBlockState(source);
            BlockState targetState = level.getBlockState(target);
            if (sourceState.hasBlockEntity() || targetState.hasBlockEntity()
                    || qBlocks.locations.has(source) || qBlocks.locations.has(target)) {
                return Status.UNSUPPORTED_BLOCKS;
            }
            if (sourceState != targetState) {
                changes.add(new Change(source, sourceState, targetState));
                changes.add(new Change(target, targetState, sourceState));
            }
        }

        int attempted = 0;
        try {
            for (Change change : changes) {
                attempted++;
                if (!level.setBlock(change.pos, change.after, SWAP_FLAGS)
                        && level.getBlockState(change.pos) != change.after) {
                    rollback(level, changes, attempted);
                    return Status.SWAP_FAILED;
                }
            }
        } catch (RuntimeException error) {
            QCraft.LOGGER.error("Quantum Computer swap failed at {}", pos, error);
            rollback(level, changes, attempted);
            return Status.SWAP_FAILED;
        }

        if (changes.stream().anyMatch(change -> level.getBlockState(change.pos) != change.after)) {
            rollback(level, changes, attempted);
            return Status.SWAP_FAILED;
        }
        coolant.setDamageValue(coolant.getDamageValue() + 1);
        computer.setChanged();
        for (Change change : changes) {
            if (level.getBlockState(change.pos) == change.after) {
                level.markAndNotifyBlock(change.pos, level.getChunkAt(change.pos), change.before, change.after, UPDATE_NEIGHBORS | UPDATE_CLIENTS, 512);
            }
        }
        playEffects(level, pos);
        playEffects(level, connection.other);
        Criteria.QUANTUM_TELEPORTATION.trigger(player, new int[]{a.getXSpan(), a.getYSpan(), a.getZSpan()});
        return Status.READY;
    }

    private static void rollback(ServerLevel level, List<Change> changes, int attempted) {
        for (int i = attempted - 1; i >= 0; i--) {
            Change change = changes.get(i);
            if (level.getBlockState(change.pos) != change.before && !level.setBlock(change.pos, change.before, SWAP_FLAGS)) {
                QCraft.LOGGER.error("Could not restore block at {} after a Quantum Computer swap", change.pos);
            }
        }
    }

    private static void playEffects(ServerLevel level, BlockPos pos) {
        level.sendParticles(QuantumOre.DUST_PARTICLE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 22, 0.55, 0.55, 0.55, 0.0);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 3.0f, 1.0f);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof QuantumComputerBlockEntity computer) {
            NetworkHooks.openScreen(serverPlayer, computer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof QuantumComputerBlockEntity computer) {
            computer.frequency().ifPresent(id -> drops.stream()
                    .filter(stack -> stack.is(asItem()))
                    .forEach(stack -> stack.getOrCreateTag().putUUID("frequency", id)));
        }
        return drops;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level instanceof ServerLevel serverLevel) {
            QuantumComputerData data = QuantumComputerData.get(serverLevel);
            var location = data.locations.get(pos);
            if (level.getBlockEntity(pos) instanceof QuantumComputerBlockEntity computer) {
                location.ifPresent(value -> computer.setFrequency(value.frequency));
                Containers.dropContents(level, pos, computer);
            }
            location.ifPresent(value -> data.remove(value));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public BiFunction<Block, Item.Properties, BlockItem> getBlockItem() {
        return QuantumComputerItem::new;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumComputerBlockEntity(pos, state);
    }
}
