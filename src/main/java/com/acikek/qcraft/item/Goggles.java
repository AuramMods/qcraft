package com.acikek.qcraft.item;

import com.acikek.qcraft.compat.CuriosGoggles;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModList;

public class Goggles extends Item {

    public enum Type {
        QUANTUM("quantum"),
        ANTI_OBSERVATION("anti_observation");

        public final String id;

        Type(String id) {
            this.id = id;
        }
    }

    public final Type type;

    public Goggles(Type type) {
        super(new Properties().stacksTo(1));
        this.type = type;
    }

    public static boolean isWearingGoggles(Player player, Type type) {
        return ModList.get().isLoaded("curios") && CuriosGoggles.isEquipped(player, type);
    }

    public static void recoverLegacyHelmet(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        ItemStack old = player.getItemBySlot(EquipmentSlot.HEAD);
        if (old.getItem() instanceof Goggles) {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(old);
        }
    }
}
