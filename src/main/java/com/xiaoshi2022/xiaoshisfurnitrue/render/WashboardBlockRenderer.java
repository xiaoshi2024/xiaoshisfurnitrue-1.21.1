package com.xiaoshi2022.xiaoshisfurnitrue.render;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WashboardBlockEntity;
import com.xiaoshi2022.xiaoshisfurnitrue.client.model.block.WashboardBlockModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WashboardBlockRenderer extends GeoBlockRenderer<WashboardBlockEntity> {
    public WashboardBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new WashboardBlockModel());
    }
}
