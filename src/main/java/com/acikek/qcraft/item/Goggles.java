package com.acikek.qcraft.item;

import com.acikek.qcraft.QCraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public class Goggles extends ArmorItem {

    public enum Type {
        QUANTUM("quantum"),
        ANTI_OBSERVATION("anti_observation");

        public final String id;

        Type(String id) {
            this.id = id;
        }
    }

    public static class Material implements ArmorMaterial {

        public final com.acikek.qcraft.item.Goggles.Type type;

        public Material(com.acikek.qcraft.item.Goggles.Type type) {
            this.type = type;
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return -1;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return 1;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_CHAIN;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return QCraft.ID + ":" + type.id + "_goggles";
        }

        @Override
        public float getToughness() {
            return 0;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    }

    public final com.acikek.qcraft.item.Goggles.Type type;

    public Goggles(com.acikek.qcraft.item.Goggles.Type type) {
        super(new Material(type), ArmorItem.Type.HELMET, new Item.Properties());
        this.type = type;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    public static boolean isWearingGoggles(Player player, com.acikek.qcraft.item.Goggles.Type type) {
        return player.hasItemInSlot(EquipmentSlot.HEAD)
                && player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof Goggles goggles
                && goggles.type == type;
    }
}
