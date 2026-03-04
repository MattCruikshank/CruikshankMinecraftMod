package com.cruikshank.mod.client;

import com.cruikshank.mod.entity.EmeraldGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class EmeraldGolemRenderer extends EntityRenderer<EmeraldGolem> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/block/emerald_block.png");

    private final BlockRenderDispatcher blockRenderer;
    private final ItemRenderer itemRenderer;

    public EmeraldGolemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.shadowRadius = 0.3F;
    }

    @Override
    public ResourceLocation getTextureLocation(EmeraldGolem entity) {
        return TEXTURE;
    }

    @Override
    public void render(EmeraldGolem entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        poseStack.pushPose();

        // Bobbing when walking
        float walkAnim = entity.walkAnimation.speed(partialTick);
        float bob = (float) Math.sin(entity.tickCount + partialTick) * 0.05F * Math.min(walkAnim, 1.0F);

        // Body: emerald block, scaled to 0.6
        poseStack.pushPose();
        poseStack.translate(-0.3, bob, -0.3); // center the 0.6-scale block
        poseStack.scale(0.6F, 0.6F, 0.6F);
        blockRenderer.renderSingleBlock(
                Blocks.EMERALD_BLOCK.defaultBlockState(),
                poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        // Head: carved pumpkin, scaled to 0.5, on top of body
        poseStack.pushPose();
        poseStack.translate(0.0, 0.6 + bob, 0.0); // on top of the body
        poseStack.translate(0.0, 0.0, 0.0);

        // Rotate pumpkin to face entity's yaw
        poseStack.translate(0.0, 0.0, 0.0);
        float yaw = entity.yBodyRot;
        poseStack.translate(0.0, 0.0, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.translate(-0.25, 0.0, -0.25);
        poseStack.scale(0.5F, 0.5F, 0.5F);

        blockRenderer.renderSingleBlock(
                Blocks.CARVED_PUMPKIN.defaultBlockState(),
                poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        // Render held item
        ItemStack held = entity.getMainHandItem();
        if (!held.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.45, 0.3 + bob, 0.0);
            poseStack.scale(0.4F, 0.4F, 0.4F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            itemRenderer.renderStatic(held, ItemDisplayContext.GROUND,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
