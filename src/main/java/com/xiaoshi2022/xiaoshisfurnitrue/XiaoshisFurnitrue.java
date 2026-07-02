package com.xiaoshi2022.xiaoshisfurnitrue;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.xiaoshi2022.xiaoshisfurnitrue.network.ModNetwork;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlocks;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModCreativeTabs;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(XiaoshisFurnitrue.MODID)
public class XiaoshisFurnitrue {
    public static final String MODID = "xiaoshisfurnitrue";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XiaoshisFurnitrue(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModNetwork.registerPackets(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("服务器启动啦！");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("小噬的家具模组加载完成！");
    }
}
