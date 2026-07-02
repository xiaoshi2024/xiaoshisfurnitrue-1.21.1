package com.xiaoshi2022.xiaoshisfurnitrue.register;

import com.xiaoshi2022.xiaoshisfurnitrue.item.WashboardItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<WashboardItem> WASHBOARD_BLOCK_ITEM = ITEMS.registerItem(
            "washboard",
            (properties) -> new WashboardItem(ModBlocks.WASHBOARD_BLOCK.get(), properties)
    );
}
