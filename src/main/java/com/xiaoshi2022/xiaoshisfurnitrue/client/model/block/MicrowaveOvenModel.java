package com.xiaoshi2022.xiaoshisfurnitrue.client.model.block;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.MicrowaveOvenBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

public class MicrowaveOvenModel extends DefaultedBlockGeoModel<MicrowaveOvenBlockEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/microwave_oven.png");

    public MicrowaveOvenModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "microwave_oven"));
    }

    @Override
    public ResourceLocation getTextureResource(MicrowaveOvenBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MicrowaveOvenBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "animations/block/microwave_oven.animation.json");
    }

    @Override
    public RenderType getRenderType(MicrowaveOvenBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}