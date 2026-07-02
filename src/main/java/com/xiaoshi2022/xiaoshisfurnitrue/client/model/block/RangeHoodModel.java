package com.xiaoshi2022.xiaoshisfurnitrue.client.model.block;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.RangeHoodBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

public class RangeHoodModel extends DefaultedBlockGeoModel<RangeHoodBlockEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/range_hood.png");

    public RangeHoodModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "range_hood"));
    }

    @Override
    public ResourceLocation getTextureResource(RangeHoodBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RangeHoodBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "animations/block/range_hood.animation.json");
    }

    @Override
    public RenderType getRenderType(RangeHoodBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
