package com.acikek.qcraft.block.quantum_computer;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class QuantumComputerMenu extends AbstractContainerMenu {

    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, QCraft.ID);
    public static final RegistryObject<MenuType<QuantumComputerMenu>> TYPE = REGISTER.register("qc_gui",
            () -> IForgeMenuType.create((id, inventory, buffer) -> new QuantumComputerMenu(id, inventory, buffer.readBlockPos())));

    public static final int ENERGIZE_BUTTON = 0;
    public static final int COOLANT_X = 19;
    public static final int COOLANT_Y = 63;
    private static final int FIRST_PLAYER_SLOT = 1;
    private static final int FIRST_HOTBAR_SLOT = 28;
    private static final int SLOT_COUNT = 37;

    private final DataSlot status = addDataSlot(DataSlot.standalone());
    private final BlockPos pos;
    @Nullable
    private final QuantumComputerBlockEntity computer;

    private QuantumComputerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, new SimpleContainer(1), null);
    }

    public QuantumComputerMenu(int id, Inventory inventory, QuantumComputerBlockEntity computer) {
        this(id, inventory, computer.getBlockPos(), computer, computer);
    }

    private QuantumComputerMenu(int id, Inventory inventory, BlockPos pos, Container coolant, @Nullable QuantumComputerBlockEntity computer) {
        super(TYPE.get(), id);
        this.pos = pos;
        this.computer = computer;
        status.set(QuantumComputer.Status.MISSING_FREQUENCY.ordinal());
        if (inventory.player.level() instanceof ServerLevel level) {
            status.set(QuantumComputer.status(level, pos).ordinal());
        }

        addSlot(new Slot(coolant, 0, COOLANT_X, COOLANT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COOLANT_CELL.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + column + row * 9, 8 + column * 18, 106 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 164));
        }
    }

    public QuantumComputer.Status getStatus() {
        return QuantumComputer.Status.values()[status.get()];
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != ENERGIZE_BUTTON || !(player instanceof ServerPlayer serverPlayer)
                || player.containerMenu != this || !stillValid(player) || computer == null) {
            return false;
        }
        QuantumComputer.Status result = QuantumComputer.energize(serverPlayer.serverLevel(), computer, serverPlayer);
        status.set(result.ordinal());
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return computer == null || computer.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (slotId == 0) {
            if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, FIRST_PLAYER_SLOT, false)) {
            boolean moved = slotId < FIRST_HOTBAR_SLOT
                    ? moveItemStackTo(stack, FIRST_HOTBAR_SLOT, SLOT_COUNT, false)
                    : moveItemStackTo(stack, FIRST_PLAYER_SLOT, FIRST_HOTBAR_SLOT, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == before.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return before;
    }
}
