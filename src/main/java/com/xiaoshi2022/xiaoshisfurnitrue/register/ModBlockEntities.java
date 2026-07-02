package com.xiaoshi2022.xiaoshisfurnitrue.register;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.MicrowaveOvenBlockEntity;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WashboardBlockEntity;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WaterDispenserBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, XiaoshisFurnitrue.MODID);

    public static final Supplier<BlockEntityType<WashboardBlockEntity>> WASHBOARD_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("washboard",
                    () -> BlockEntityType.Builder.of(WashboardBlockEntity::new,
                            ModBlocks.WASHBOARD_BLOCK.get()).build(null)
            );

    // 在 BlockEntity 注册时设置 tick
    public static final Supplier<BlockEntityType<MicrowaveOvenBlockEntity>> MICROWAVE_OVEN_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("microwave_oven",
                    () -> BlockEntityType.Builder.of(
                            MicrowaveOvenBlockEntity::new,
                            ModBlocks.MICROWAVE_OVEN_BLOCK.get()
                    ).build(null)
            );

    // 添加饮水机
    public static final Supplier<BlockEntityType<WaterDispenserBlockEntity>> WATER_DISPENSER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("water_dispenser",
                    () -> BlockEntityType.Builder.of(WaterDispenserBlockEntity::new,
                            ModBlocks.WATER_DISPENSER_BLOCK.get()).build(null)
            );
}