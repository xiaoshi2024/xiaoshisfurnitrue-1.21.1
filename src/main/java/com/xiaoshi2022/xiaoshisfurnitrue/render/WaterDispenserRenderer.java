package com.xiaoshi2022.xiaoshisfurnitrue.render;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WaterDispenserBlockEntity;
import com.xiaoshi2022.xiaoshisfurnitrue.client.model.block.WaterDispenserModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WaterDispenserRenderer extends GeoBlockRenderer<WaterDispenserBlockEntity> {
    public WaterDispenserRenderer() {
        super(new WaterDispenserModel());
    }
}