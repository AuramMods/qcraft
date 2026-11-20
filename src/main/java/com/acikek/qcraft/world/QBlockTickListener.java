package com.acikek.qcraft.world;

import com.acikek.qcraft.block.AutomatedObserver;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.item.Goggles;
import com.acikek.qcraft.world.state.QBlockData;
import com.acikek.qcraft.world.state.location.QBlockLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QBlockTickListener {

    private static final double PITCH_THRESHOLD = 60.0;
    private static final double YAW_THRESHOLD = 80.0;

    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.level instanceof ServerLevel level) {
            observeWorld(level);
        }
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && QBlockData.get(level).locations.has(event.getPos())) {
            event.setExpToDrop(0);
        }
    }

    public static void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var structure = event.getStructureHelper();
        if (structure == null || !structure.resolve()) {
            return;
        }
        QBlockData data = QBlockData.get(level);
        Direction move = event.getPistonMoveType().isExtend ? event.getDirection() : event.getDirection().getOpposite();
        if (data.locations.has(event.getFaceOffsetPos())
                || structure.getToDestroy().stream().anyMatch(data.locations::has)
                || structure.getToPush().stream().anyMatch(pos -> data.locations.has(pos) || data.locations.has(pos.relative(move)))) {
            event.setCanceled(true);
        }
    }

    // Keep the original angular field of view independent of the client's graphics settings.
    static boolean canSee(Player player, Vec3 between) {
        double pitch = Math.toDegrees(Math.atan2(between.y, Math.hypot(between.x, between.z)));
        Vec3 rotated = between.yRot((float) Math.toRadians(270));
        double yaw = Math.toDegrees(Math.atan2(rotated.z, rotated.x));
        double yawDiff = Math.abs(Mth.wrapDegrees(player.getYRot() - yaw));
        double playerPitch = player.getXRot();
        if (yawDiff > 90.0) {
            pitch = (playerPitch > 0.0 ? 180.0 : -180.0) - pitch;
        }
        boolean verticalView = playerPitch + PITCH_THRESHOLD > 90 || playerPitch - PITCH_THRESHOLD < -90;
        return Math.abs(playerPitch - pitch) < PITCH_THRESHOLD && (verticalView || yawDiff < YAW_THRESHOLD);
    }

    private static void observeWorld(ServerLevel level) {
        QBlockData data = QBlockData.get(level);
        List<QBlockLocation> loaded = data.getLoadedLocations(level);
        if (loaded.isEmpty()) {
            return;
        }

        int range = level.getServer().getPlayerList().getSimulationDistance() * 16;
        Set<QBlockLocation> held = new HashSet<>();
        for (QBlockLocation location : loaded) {
            data.applyPendingDisplay(location, level);
            Direction machine = poweredObserver(level, location);
            if (machine != null) {
                held.add(location);
                if (data.isUnobserved(location)) {
                    Vec3 center = Vec3.atCenterOf(location.pos);
                    Player nearby = level.getNearestPlayer(center.x, center.y, center.z, 10.0, false);
                    data.observe(location, level, location.type.pickFace(machine, level.random), QBlock.Observation.AUTOMATED_OBSERVER, nearby);
                }
            }

            for (Player player : level.players()) {
                if (player.isSpectator() || Goggles.isWearingGoggles(player, Goggles.Type.ANTI_OBSERVATION)) {
                    continue;
                }
                if (location.pos.closerToCenterThan(player.getEyePosition(), range) && canSee(player, location.getBetween(player.getEyePosition()))) {
                    held.add(location);
                    if (data.isUnobserved(location)) {
                        data.observe(location, level, player);
                    }
                } else if (location.pos.closerToCenterThan(player.position().lerp(player.getEyePosition(), 0.5), 2.0)) {
                    held.add(location);
                }
            }
        }

        for (QBlockLocation location : loaded) {
            QBlockLocation other = data.frequencies.counterpart(location);
            if (!held.contains(location) && !held.contains(other)) {
                if (location.observed) {
                    data.unobserve(location, level);
                } else if (other != null && other.observed) {
                    data.unobserve(other, level);
                }
            }
        }
    }

    private static Direction poweredObserver(ServerLevel level, QBlockLocation location) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            var observerPos = location.pos.relative(face);
            if (level.hasChunkAt(observerPos)) {
                BlockState state = level.getBlockState(observerPos);
                if (state.getBlock() instanceof AutomatedObserver
                        && state.getValue(AutomatedObserver.FACING) == face
                        && state.getValue(AutomatedObserver.POWERED)) {
                    return face;
                }
            }
        }
        return null;
    }
}
