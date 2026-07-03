package com.xiaoshi2022.xiaoshisfurnitrue.register;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModPOIs {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, XiaoshisFurnitrue.MODID);

    public static final Supplier<PoiType> FURNITURE_MERCHANT_POI = POI_TYPES.register("furniture_merchant",
            () -> new PoiType(
                    // 🔥 改成微波炉！
                    java.util.Set.copyOf(ModBlocks.MICROWAVE_OVEN_BLOCK.get().getStateDefinition().getPossibleStates()),
                    1,  // 有效范围
                    8   // 最大数量
            ));
}