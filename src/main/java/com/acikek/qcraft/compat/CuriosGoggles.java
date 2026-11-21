package com.acikek.qcraft.compat;

import com.acikek.qcraft.item.Goggles;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosGoggles {

    public static final String SLOT = "goggles";

    private CuriosGoggles() {
    }

    public static boolean isEquipped(Player player, Goggles.Type type) {
        return CuriosApi.getCuriosInventory(player).map(handler -> handler.findCurios(SLOT).stream()
                .anyMatch(result -> result.stack().getItem() instanceof Goggles goggles && goggles.type == type))
                .orElse(false);
    }
}
