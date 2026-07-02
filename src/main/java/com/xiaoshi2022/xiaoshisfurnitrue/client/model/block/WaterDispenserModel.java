package com.xiaoshi2022.xiaoshisfurnitrue.client.model.block;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WaterDispenserBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

public class WaterDispenserModel extends DefaultedBlockGeoModel<WaterDispenserBlockEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/water_dispenser.png");

    public WaterDispenserModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "water_dispenser"));
    }

    @Override
    public ResourceLocation getTextureResource(WaterDispenserBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WaterDispenserBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "animations/block/water_dispenser.animation.json");
    }

    @Override
    public RenderType getRenderType(WaterDispenserBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}