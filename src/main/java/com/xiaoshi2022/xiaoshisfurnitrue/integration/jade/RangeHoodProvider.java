package com.xiaoshi2022.xiaoshisfurnitrue.integration.jade;

import com.xiaoshi2022.xiaoshisfurnitrue.block.RangeHoodBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

public enum RangeHoodProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        return currentIcon;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        boolean powered = state.getValue(RangeHoodBlock.POWERED);
        boolean open = state.getValue(RangeHoodBlock.OPEN);

        IElementHelper helper = IElementHelper.get();  // 修改这里
        IElement powerIcon = helper.item(new ItemStack(powered ? Items.LIME_DYE : Items.GRAY_DYE), 0.75f)
                .size(new net.minecraft.world.phys.Vec2(12, 12))
                .translate(new net.minecraft.world.phys.Vec2(0, -1));
        powerIcon.message(null);
        tooltip.add(powerIcon);
        tooltip.append(Component.translatable("jade.xiaoshisfurnitrue.range_hood.power",
                powered ? Component.translatable("jade.xiaoshisfurnitrue.on") :
                        Component.translatable("jade.xiaoshisfurnitrue.off")));

        IElement ventIcon = helper.item(new ItemStack(Items.IRON_TRAPDOOR), 0.75f)
                .size(new net.minecraft.world.phys.Vec2(12, 12))
                .translate(new net.minecraft.world.phys.Vec2(0, -1));
        ventIcon.message(null);
        tooltip.add(ventIcon);
        tooltip.append(Component.translatable("jade.xiaoshisfurnitrue.range_hood.vent",
                open ? Component.translatable("jade.xiaoshisfurnitrue.open") :
                        Component.translatable("jade.xiaoshisfurnitrue.closed")));
    }

    @Override
    public net.minecraft.resources.ResourceLocation getUid() {
        return XiaoshisJadePlugin.RANGE_HOOD_STATUS;
    }
}