package com.xiaoshi2022.xiaoshisfurnitrue.client.model.item;

import com.xiaoshi2022.xiaoshisfurnitrue.item.WashboardItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class WashboardItemModel extends DefaultedItemGeoModel<WashboardItem> {
    public WashboardItemModel() {
        super(ResourceLocation.fromNamespaceAndPath("xiaoshisfurnitrue", "washboard_item"));
    }

    @Override
    public ResourceLocation getTextureResource(WashboardItem item) {
        return ResourceLocation.fromNamespaceAndPath("xiaoshisfurnitrue", "textures/item/washboard_item.png");
    }
}
