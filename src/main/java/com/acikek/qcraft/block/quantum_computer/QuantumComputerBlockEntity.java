package com.acikek.qcraft.block.quantum_computer;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.Blocks;
import com.acikek.qcraft.item.Items;
import com.acikek.qcraft.world.state.QuantumComputerData;
import lib.ImplementedInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class QuantumComputerBlockEntity extends BlockEntity implements ImplementedInventory, WorldlyContainer, MenuProvider {

    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, QCraft.ID);
    public static final RegistryObject<BlockEntityType<QuantumComputerBlockEntity>> TYPE = REGISTER.register("quantum_computer_block_entity",
            () -> BlockEntityType.Builder.of(QuantumComputerBlockEntity::new, Blocks.QUANTUM_COMPUTER.get()).build(null));

    private static final int[] COOLANT_SLOT = {0};
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private Optional<UUID> frequency = Optional.empty();
    private LazyOptional<IItemHandler> hopperAccess = createHopperAccess();

    public QuantumComputerBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE.get(), pos, state);
    }

    Optional<UUID> frequency() {
        if (level instanceof ServerLevel serverLevel && !isRemoved()) {
            var location = QuantumComputerData.get(serverLevel).locations.get(worldPosition);
            if (location.isPresent()) {
                return location.get().frequency;
            }
        }
        return frequency;
    }

    void setFrequency(Optional<UUID> value) {
        frequency = value;
        setChanged();
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ImplementedInventory.super.setItem(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        frequency = tag.hasUUID("frequency") ? Optional.of(tag.getUUID("frequency")) : Optional.empty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        frequency.ifPresent(id -> tag.putUUID("frequency", id));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new QuantumComputerMenu(syncId, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? COOLANT_SLOT : new int[0];
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.is(Items.COOLANT_CELL.get());
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side == Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    private LazyOptional<IItemHandler> createHopperAccess() {
        return LazyOptional.of(() -> new SidedInvWrapper(this, Direction.UP));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && side == Direction.UP && !isRemoved()) {
            return hopperAccess.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hopperAccess.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        hopperAccess = createHopperAccess();
    }
}
