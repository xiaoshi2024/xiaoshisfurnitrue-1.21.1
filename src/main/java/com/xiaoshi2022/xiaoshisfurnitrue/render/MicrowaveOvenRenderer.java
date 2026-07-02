package com.xiaoshi2022.xiaoshisfurnitrue.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaoshi2022.xiaoshisfurnitrue.block.MicrowaveOvenBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.MicrowaveOvenBlockEntity;
import com.xiaoshi2022.xiaoshisfurnitrue.client.model.block.MicrowaveOvenModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MicrowaveOvenRenderer extends GeoBlockRenderer<MicrowaveOvenBlockEntity> {
    public MicrowaveOvenRenderer(BlockEntityRendererProvider.Context context) {
        super(new MicrowaveOvenModel());
    }

    public MicrowaveOvenRenderer() {
        super(new MicrowaveOvenModel());
    }

    @Override
    public void render(MicrowaveOvenBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 先渲染 GeckoLib 方块模型和动画
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 取出食物，为空就不用渲染了
        ItemStack foodStack = blockEntity.getFood();
        if (foodStack.isEmpty()) {
            return;
        }

        // 获取方块朝向
        Direction facing = blockEntity.getBlockState().getValue(MicrowaveOvenBlock.FACING);

        // 获取物品渲染器
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(foodStack, blockEntity.getLevel(), null, 0);

        poseStack.pushPose();

        // 定位到方块中心
        poseStack.translate(0.5D, 0.5D, 0.5D);

        // 根据朝向做Y轴旋转，确保食物在微波炉腔内
        float rotationY = switch (facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        // 调整到微波炉腔内高度（大概中间偏低的位置）
        poseStack.translate(0.0D, -0.15D, 0.0D);

        // 稍微缩小一点让食物看起来像在腔内
        float scale = 0.55F;
        poseStack.scale(scale, scale, scale);

        // 让食物稍微有点旋转感（增加真实感）
        long time = System.currentTimeMillis();
        float spin = (time % 4000L) / 4000.0F * 360.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        // 渲染物品
        int light = LightTexture.FULL_BRIGHT;
        itemRenderer.render(
                foodStack,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                bufferSource,
                light,
                OverlayTexture.NO_OVERLAY,
                model
        );

        poseStack.popPose();
    }
}
