package com.xiaoshi2022.xiaoshisfurnitrue.render;

import com.xiaoshi2022.xiaoshisfurnitrue.client.model.item.WashboardItemModel;
import com.xiaoshi2022.xiaoshisfurnitrue.item.WashboardItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class WashboardRenderer extends GeoItemRenderer<WashboardItem> {
    public WashboardRenderer() {
        super(new WashboardItemModel());
    }
}
