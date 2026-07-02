package com.xiaoshi2022.xiaoshisfurnitrue.client.model.block;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WashboardBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

public class WashboardBlockModel extends DefaultedBlockGeoModel<WashboardBlockEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/washboard.png");

    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MODID, "animations/block/washboard.animation.json");

    public WashboardBlockModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "washboard"));
    }

    @Override
    public ResourceLocation getTextureResource(WashboardBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WashboardBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(WashboardBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    // 可选：自定义动画处理
    @Override
    public void setCustomAnimations(WashboardBlockEntity animatable, long instanceId, AnimationState<WashboardBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}