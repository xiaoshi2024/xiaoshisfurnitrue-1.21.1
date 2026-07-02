package com.xiaoshi2022.xiaoshisfurnitrue.register;

import com.xiaoshi2022.xiaoshisfurnitrue.block.WashboardBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.block.WaterDispenserBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    // 修复：使用 registerBlock() 而不是 register()
    public static final DeferredBlock<WashboardBlock> WASHBOARD_BLOCK = BLOCKS.registerBlock(
            "washboard",
            WashboardBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(net.minecraft.world.level.block.SoundType.WOOD)
                    .noOcclusion()
    );

    public static final DeferredBlock<WaterDispenserBlock> WATER_DISPENSER_BLOCK = BLOCKS.registerBlock(
            "water_dispenser",
            WaterDispenserBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );
}