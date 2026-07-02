package com.xiaoshi2022.xiaoshisfurnitrue.client;

import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import com.xiaoshi2022.xiaoshisfurnitrue.render.WashboardBlockRenderer;
import com.xiaoshi2022.xiaoshisfurnitrue.render.WaterDispenserRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientRegistry {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WASHBOARD_BLOCK_ENTITY.get(), WashboardBlockRenderer::new);
// 饮水机渲染器
        event.registerBlockEntityRenderer(ModBlockEntities.WATER_DISPENSER_BLOCK_ENTITY.get(),
                ctx -> new WaterDispenserRenderer());

    }
}
