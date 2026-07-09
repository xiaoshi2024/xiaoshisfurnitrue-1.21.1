package com.xiaoshi2022.xiaoshisfurnitrue;

import com.xiaoshi2022.xiaoshisfurnitrue.network.ModNetwork;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WaterDispenserBlockEntity;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlocks;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModCreativeTabs;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModItems;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModPOIs;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModVillagers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;

@Mod(XiaoshisFurnitrue.MODID)
public class XiaoshisFurnitrue {
    public static final String MODID = "xiaoshisfurnitrue";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XiaoshisFurnitrue(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModNetwork.registerPackets(modEventBus);
        ModPOIs.POI_TYPES.register(modEventBus);
        ModVillagers.VILLAGER_PROFESSIONS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new ModVillagers());

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("服务器启动啦！");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("小噬的家具模组加载完成！");
    }

    public void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WATER_DISPENSER_BLOCK_ENTITY.get(),
                (blockEntity, context) -> (net.neoforged.neoforge.fluids.capability.IFluidHandler) blockEntity
        );
    }
}
