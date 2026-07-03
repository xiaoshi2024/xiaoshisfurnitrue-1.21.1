package com.xiaoshi2022.xiaoshisfurnitrue.integration.jade;

import com.xiaoshi2022.xiaoshisfurnitrue.block.WaterDispenserBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

public enum WaterDispenserProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        return currentIcon;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        int level = state.getValue(WaterDispenserBlock.WATER_LEVEL);
        boolean hasWater = state.getValue(WaterDispenserBlock.HAS_WATER);

        IElementHelper helper = IElementHelper.get();  // 修改这里
        IElement icon = helper.item(WaterDispenserItemIcon.getStack(), 0.75f)
                .size(new net.minecraft.world.phys.Vec2(14, 14))
                .translate(new net.minecraft.world.phys.Vec2(0, -2));
        icon.message(null);
        tooltip.add(icon);
        tooltip.append(Component.translatable("jade.xiaoshisfurnitrue.water_level", level, 3));
    }

    @Override
    public net.minecraft.resources.ResourceLocation getUid() {
        return XiaoshisJadePlugin.WATER_LEVEL;
    }

    private static class WaterDispenserItemIcon {
        private static net.minecraft.world.item.ItemStack cached;

        static net.minecraft.world.item.ItemStack getStack() {
            if (cached == null) {
                cached = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
            }
            return cached;
        }
    }
}