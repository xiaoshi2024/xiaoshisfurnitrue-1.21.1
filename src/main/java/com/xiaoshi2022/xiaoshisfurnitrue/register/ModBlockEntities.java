package com.xiaoshi2022.xiaoshisfurnitrue.register;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WashboardBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

import static com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue.MODID;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final Supplier<BlockEntityType<WashboardBlockEntity>> WASHBOARD_BLOCK_ENTITY = BLOCK_ENTITIES.register("washboard",
            () -> new BlockEntityType<>(WashboardBlockEntity::new, Set.of(ModBlocks.WASHBOARD_BLOCK.get()), null));
}
