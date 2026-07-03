package com.xiaoshi2022.xiaoshisfurnitrue.integration.jade;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import com.xiaoshi2022.xiaoshisfurnitrue.block.MicrowaveOvenBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.block.RangeHoodBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.block.WaterDispenserBlock;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(XiaoshisJadePlugin.ID)
public class XiaoshisJadePlugin implements IWailaPlugin {

    public static final String ID = XiaoshisFurnitrue.MODID;

    public static final ResourceLocation WATER_LEVEL = ResourceLocation.fromNamespaceAndPath(ID, "water_level");
    public static final ResourceLocation MICROWAVE_COOK_PROGRESS = ResourceLocation.fromNamespaceAndPath(ID, "microwave_cook_progress");
    public static final ResourceLocation RANGE_HOOD_STATUS = ResourceLocation.fromNamespaceAndPath(ID, "range_hood_status");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MicrowaveOvenProvider.INSTANCE,
                com.xiaoshi2022.xiaoshisfurnitrue.block.entity.MicrowaveOvenBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(WaterDispenserProvider.INSTANCE, WaterDispenserBlock.class);
        registration.registerBlockComponent(MicrowaveOvenProvider.INSTANCE, MicrowaveOvenBlock.class);
        registration.registerBlockComponent(RangeHoodProvider.INSTANCE, RangeHoodBlock.class);
    }
}
