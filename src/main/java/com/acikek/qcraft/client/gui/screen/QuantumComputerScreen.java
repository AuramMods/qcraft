package com.acikek.qcraft.client.gui.screen;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.quantum_computer.QuantumComputer;
import com.acikek.qcraft.block.quantum_computer.QuantumComputerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class QuantumComputerScreen extends AbstractContainerScreen<QuantumComputerMenu> {

    private static final ResourceLocation SUCCESS = QCraft.id("textures/gui/checkmark_indicator.png");
    private static final ResourceLocation WARNING = QCraft.id("textures/gui/warning_indicator.png");
    private static final ResourceLocation COOLANT = QCraft.id("textures/gui/coolant_cell_slot.png");

    public QuantumComputerScreen(QuantumComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 188;
        inventoryLabelY = 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.qcraft.energize"), button ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, QuantumComputerMenu.ENERGIZE_BUTTON))
                .bounds(leftPos + 46, topPos + 61, 113, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xff333333);

        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff373737);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
        if (!menu.getSlot(0).hasItem()) {
            graphics.blit(COOLANT, leftPos + QuantumComputerMenu.COOLANT_X, topPos + QuantumComputerMenu.COOLANT_Y,
                    0, 0, 16, 16, 16, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        QuantumComputer.Status status = menu.getStatus();
        graphics.blit(status == QuantumComputer.Status.READY ? SUCCESS : WARNING, 8, 27, 0, 0, 16, 16, 16, 16);
        graphics.drawWordWrap(font, status.text(), 30, 28, 136, 0x404040);
    }
}
