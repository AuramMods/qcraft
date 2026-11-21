package com.acikek.qcraft.world.state.location;

import com.acikek.qcraft.block.qblock.InertQBlock;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.world.state.QBlockData;
import com.acikek.qcraft.world.state.frequency.Frequential;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class QBlockLocation extends Frequential {

    public static final int INERT = QBlock.Face.values().length;

    public static final Codec<QBlockLocation> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(l -> l.pos),
                    UUIDUtil.CODEC.optionalFieldOf("frequency").forGetter(l -> l.frequency),
                    QBlock.Type.CODEC.fieldOf("type").forGetter(l -> l.type),
                    Codec.list(Codec.STRING).fieldOf("faces").forGetter(l -> l.faces),
                    Codec.BOOL.fieldOf("observed").forGetter(l -> l.observed),
                    Codec.intRange(-1, INERT).optionalFieldOf("pending_face", -1).forGetter(l -> l.pendingFace),
                    Codec.intRange(-1, INERT).optionalFieldOf("displayed_face", -1).forGetter(l -> l.displayedFace),
                    CompoundTag.CODEC.optionalFieldOf("face_data", new CompoundTag()).forGetter(l -> l.faceData)
            )
            .apply(instance, QBlockLocation::new)
    );

    public static final Codec<List<QBlockLocation>> LIST_CODEC = Codec.list(CODEC);

    public final QBlock.Type type;
    public final List<String> faces;
    public boolean observed;
    public int pendingFace;
    public int displayedFace;
    private final CompoundTag faceData;

    /**
     * Constructs a {@link QBlockLocation}.<br>
     * To add a location to a {@link QBlockData} instance, see {@link QBlockData#add(QBlock.Type, BlockPos, ItemStack)}.
     *
     * @param type      The qBlock's type. This determines its observation behavior.
     * @param pos       The block position of the location.
     * @param faces     The block face IDs. To read these from an item, see {@link QBlockItem#getFaces(ItemStack)}.
     * @param faceData  Block entity data carried by the configured qBlock item.
     * @param frequency The String UUID of the location's entanglement frequency.
     */
    public QBlockLocation(BlockPos pos, Optional<UUID> frequency, QBlock.Type type, List<String> faces, CompoundTag faceData) {
        this(pos, frequency, type, faces, false, -1, INERT, faceData);
    }

    private QBlockLocation(BlockPos pos, Optional<UUID> frequency, QBlock.Type type, List<String> faces, boolean observed,
                           int pendingFace, int displayedFace, CompoundTag faceData) {
        super(pos, frequency);
        this.pendingFace = pendingFace;
        this.displayedFace = displayedFace;
        this.type = type;
        this.faces = List.copyOf(faces);
        this.observed = observed;
        this.faceData = faceData.copy();
    }

    public Block getFaceBlock(int index) {
        ResourceLocation id = index < faces.size() ? ResourceLocation.tryParse(faces.get(index)) : null;
        return id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(id);
    }

    /**
     * @return The block at the specified face.
     * @see QBlockLocation#getFaceBlock(QBlock.Face)
     */
    public Block getFaceBlock(QBlock.Face face) {
        return getFaceBlock(face.index);
    }

    public BlockState getFaceState(int face) {
        if (face == INERT) {
            return type.resolveInert().defaultBlockState();
        }
        Block block = getFaceBlock(face);
        CompoundTag data = faceData.getCompound(QBlock.Face.values()[face].name());
        if (data.contains("state", Tag.TAG_COMPOUND)) {
            BlockState saved = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), data.getCompound("state"));
            if (saved.is(block) && saved.hasBlockEntity()) {
                return saved;
            }
        }
        return block.defaultBlockState();
    }

    public boolean captureFace(BlockState state, BlockEntity entity) {
        if (entity == null || entity.onlyOpCanSetNbt()) {
            return false;
        }
        int face = displayedFace;
        if (face < 0 || face >= INERT || !state.is(getFaceBlock(face))) {
            // Older worlds did not record which face is displayed. Preserve its live tile once.
            face = 0;
            while (face < INERT && !state.is(getFaceBlock(face))) {
                face++;
            }
            if (face == INERT) {
                return false;
            }
        }
        CompoundTag snapshot = new CompoundTag();
        snapshot.put("state", NbtUtils.writeBlockState(state));
        snapshot.put("entity", entity.saveWithId().copy());
        faceData.put(QBlock.Face.values()[face].name(), snapshot);
        displayedFace = face;
        return true;
    }

    public void restoreFace(int face, Level level, BlockEntity entity) {
        if (entity.onlyOpCanSetNbt()) {
            return;
        }
        CompoundTag data = faceData.getCompound(QBlock.Face.values()[face].name());
        if (data.contains("entity", Tag.TAG_COMPOUND)) {
            CompoundTag snapshot = data.getCompound("entity");
            if (!BlockEntityType.getKey(entity.getType()).toString().equals(snapshot.getString("id"))) {
                throw new IllegalStateException("Saved block entity type does not match " + entity.getType());
            }
            entity.load(snapshot.copy());
        } else {
            BlockState state = level.getBlockState(pos);
            ItemStack seed = data.contains("item", Tag.TAG_COMPOUND) ? ItemStack.of(data.getCompound("item")) : new ItemStack(state.getBlock());
            if (seed.getItem() instanceof BlockItem item && state.is(item.getBlock())) {
                BlockItem.updateCustomBlockEntityTag(level, null, pos, seed);
                state.getBlock().setPlacedBy(level, pos, state, null, seed);
            }
        }
        entity.setChanged();
    }

    /**
     * @return The stack, based on block type, with the faces applied to its NBT.
     * @see QBlockItem#applyFaces(ItemStack, List)
     */
    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(type.resolveBlock());
        QBlockItem.applyFaces(stack, faces);
        QBlockItem.applyFaceData(stack, faceData);
        frequency.ifPresent(f -> stack.getOrCreateTag().putUUID("frequency", f));
        return stack;
    }

    /**
     * @return The vector between the block position and a player's eye position.
     */
    public Vec3 getBetween(Vec3 eyePos) {
        return eyePos.subtract(Vec3.atCenterOf(pos));
    }

    /**
     * @return The closest axis to the player.
     * @see QBlockLocation#getBetween(Vec3)
     */
    public QBlock.Axis getClosestAxis(Vec3 dists) {
        double absX = Math.abs(dists.x);
        double absY = Math.abs(dists.y);
        double absZ = Math.abs(dists.z);
        if (absX > absY && absX > absZ) {
            return QBlock.Axis.X;
        }
        else if (absY > absX && absY > absZ) {
            return QBlock.Axis.Y;
        }
        else {
            return QBlock.Axis.Z;
        }
    }

    /**
     * @return The closest block face to the player.
     * @see QBlockLocation#getBetween(Vec3)
     */
    public QBlock.Face getClosestFace(Vec3 dists) {
        return switch (getClosestAxis(dists)) {
            case X -> dists.x > 0 ? QBlock.Face.EAST : QBlock.Face.WEST;
            case Y -> dists.y > 0 ? QBlock.Face.UP : QBlock.Face.DOWN;
            case Z -> dists.z > 0 ? QBlock.Face.SOUTH : QBlock.Face.NORTH;
        };
    }

    public QBlock.Face pickFace(Player player, Level world) {
        Vec3 dists = getBetween(player.getEyePosition());
        return type.pickFace(this, dists, world.random);
    }

    /**
     * @return Whether the specified block state is impossible given the valid block faces.
     */
    public boolean isStateImpossible(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof InertQBlock) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        return !faces.contains(id);
    }

    public boolean isPylonBase(QBlock.Face face) {
        if (type != QBlock.Type.OBSERVER_DEPENDENT || faces.size() != 6) {
            return false;
        }
        for (int i = 0; i < faces.size(); i++) {
            Block block = getFaceBlock(i);
            if ((face.index == i && block != Blocks.GOLD_BLOCK) || (face.index != i && block != Blocks.OBSIDIAN)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "qBlock (" + type + ", " + pos.toShortString() + ")";
    }

    /**
     * Represents a pair of entangled qBlocks.
     */
    public static class Pair extends com.acikek.qcraft.world.state.frequency.Pair<QBlockLocation> {

        public static final Codec<Pair> CODEC = getCodec(QBlockLocation.CODEC, Pair::new);

        public Pair(QBlockLocation first) {
            super(first);
        }

        public Pair(Optional<QBlockLocation> left, Optional<QBlockLocation> right) {
            super(left, right);
        }

        public boolean getOtherObserved(QBlockLocation location) {
            QBlockLocation other = getOther(location);
            if (other != null) {
                return other.observed;
            }
            return false;
        }
    }
}
