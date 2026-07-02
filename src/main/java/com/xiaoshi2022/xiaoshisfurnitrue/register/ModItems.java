package com.xiaoshi2022.xiaoshisfurnitrue.register;

import com.xiaoshi2022.xiaoshisfurnitrue.item.WashboardItem;
import net.minecraft.world.item.BlockItem;
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

    public static final DeferredItem<BlockItem> MICROWAVE_OVEN_ITEM = ITEMS.register(
            "microwave_oven",
            () -> new BlockItem(ModBlocks.MICROWAVE_OVEN_BLOCK.get(), new Item.Properties())
    );

    // 饮水机物品 - 使用 register 而不是 registerBlockItem
    public static final DeferredItem<BlockItem> WATER_DISPENSER_ITEM = ITEMS.register(
            "water_dispenser",
            () -> new BlockItem(ModBlocks.WATER_DISPENSER_BLOCK.get(), new Item.Properties())
    );
}
