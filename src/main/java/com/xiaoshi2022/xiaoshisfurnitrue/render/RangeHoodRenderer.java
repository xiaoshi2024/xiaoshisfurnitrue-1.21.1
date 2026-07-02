package com.xiaoshi2022.xiaoshisfurnitrue.render;

import com.xiaoshi2022.xiaoshisfurnitrue.client.model.block.RangeHoodModel;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.RangeHoodBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class RangeHoodRenderer extends GeoBlockRenderer<RangeHoodBlockEntity> {
    public RangeHoodRenderer(BlockEntityRendererProvider.Context context) {
        super(new RangeHoodModel());
    }

    public RangeHoodRenderer() {
        super(new RangeHoodModel());
    }
}
