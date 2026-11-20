package com.acikek.qcraft.item;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;

public class Essence extends Item {

    public enum Type {
        OBSERVATION,
        SUPERPOSITION,
        ENTANGLEMENT
    }

    public final Type essenceType;

    public Essence(Properties settings, Type essenceType) {
        super(settings);
        this.essenceType = essenceType;
    }

    public static int findSlot(CraftingContainer inventory) {
        if (inventory.isEmpty()) {
            return -1;
        }
        int essenceSlot = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof Essence) {
                essenceSlot = i;
                break;
            }
        }
        return essenceSlot;
    }
}
