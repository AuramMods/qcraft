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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class QBlockLocation extends Frequential {

    public static final Codec<QBlockLocation> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(l -> l.pos),
                    UUIDUtil.CODEC.optionalFieldOf("frequency").forGetter(l -> l.frequency),
                    QBlock.Type.CODEC.fieldOf("type").forGetter(l -> l.type),
                    Codec.list(Codec.STRING).fieldOf("faces").forGetter(l -> l.faces),
                    Codec.BOOL.fieldOf("observed").forGetter(l -> l.observed),
                    Codec.intRange(-1, 6).optionalFieldOf("pending_face", -1).forGetter(l -> l.pendingFace)
            )
            .apply(instance, QBlockLocation::new)
    );

    public static final Codec<List<QBlockLocation>> LIST_CODEC = Codec.list(CODEC);

    public final QBlock.Type type;
    public final List<String> faces;
    public boolean observed;
    public int pendingFace;

    /**
     * Constructs a {@link QBlockLocation}.<br>
     * To add a location to a {@link QBlockData} instance, see {@link QBlockData#add(QBlock.Type, BlockPos, ItemStack)}.
     *
     * @param type      The qBlock's type. This determines its observation behavior.
     * @param pos       The block position of the location.
     * @param faces     The block face IDs. To read these from an item, see {@link QBlockItem#getFaces(ItemStack)}.
     * @param observed  Whether this location is currently observed.
     * @param frequency The String UUID of the location's entanglement frequency.
     */
    public QBlockLocation(BlockPos pos, Optional<UUID> frequency, QBlock.Type type, List<String> faces, boolean observed) {
        this(pos, frequency, type, faces, observed, -1);
    }

    private QBlockLocation(BlockPos pos, Optional<UUID> frequency, QBlock.Type type, List<String> faces, boolean observed, int pendingFace) {
        super(pos, frequency);
        this.pendingFace = pendingFace;
        this.type = type;
        this.faces = faces;
        this.observed = observed;
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

    /**
     * @return The stack, based on block type, with the faces applied to its NBT.
     * @see QBlockItem#applyFaces(ItemStack, List)
     */
    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(type.resolveBlock());
        QBlockItem.applyFaces(stack, faces);
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
