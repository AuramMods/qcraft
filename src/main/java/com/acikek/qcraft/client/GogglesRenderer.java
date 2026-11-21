package com.acikek.qcraft.client;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.compat.CuriosGoggles;
import com.acikek.qcraft.item.Items;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

final class GogglesRenderer implements ICurioRenderer {

    private final ResourceLocation texture;
    private final ModelPart head;

    private GogglesRenderer(String type) {
        texture = QCraft.id("textures/models/goggles/" + type + ".png");
        // Keep the glasses just outside the helmet layer when both are worn.
        head = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.1f), 0), 64, 32)
                .bakeRoot().getChild("head");
    }

    static void register() {
        CuriosRendererRegistry.register(Items.QUANTUM_GOGGLES.get(), () -> new GogglesRenderer(Items.QUANTUM_GOGGLES.get().type.id));
        CuriosRendererRegistry.register(Items.ANTI_OBSERVATION_GOGGLES.get(), () -> new GogglesRenderer(Items.ANTI_OBSERVATION_GOGGLES.get().type.id));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot,
            PoseStack pose, RenderLayerParent<T, M> renderer, MultiBufferSource buffers, int light,
            float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
        if (CuriosGoggles.SLOT.equals(slot.identifier()) && renderer.getModel() instanceof HumanoidModel<?> model) {
            head.copyFrom(model.head);
            head.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
        }
    }
}
