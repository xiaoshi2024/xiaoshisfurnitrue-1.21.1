package com.xiaoshi2022.xiaoshisfurnitrue.integration.jade;

import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.MicrowaveOvenBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

public enum MicrowaveOvenProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final String KEY_COOK_PROGRESS = "CookProgress";
    public static final String KEY_COOKING = "IsCooking";
    public static final String KEY_HAS_FOOD = "HasFood";

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        return currentIcon;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        boolean hasFood = data.getBoolean(KEY_HAS_FOOD);
        if (hasFood) {
            IElementHelper helper = IElementHelper.get();  // 修改这里
            IElement foodIcon = helper.item(new ItemStack(Items.CARROT), 0.75f)
                    .size(new net.minecraft.world.phys.Vec2(12, 12))
                    .translate(new net.minecraft.world.phys.Vec2(0, -1));
            foodIcon.message(null);
            tooltip.add(foodIcon);
            tooltip.append(Component.translatable("jade.xiaoshisfurnitrue.microwave.has_food"));
        }

        boolean cooking = data.getBoolean(KEY_COOKING);
        if (cooking && data.contains(KEY_COOK_PROGRESS)) {
            int progress = data.getInt(KEY_COOK_PROGRESS);
            tooltip.add(Component.translatable("jade.xiaoshisfurnitrue.microwave.cook_progress", progress));
        } else if (!hasFood) {
            tooltip.add(Component.translatable("jade.xiaoshisfurnitrue.microwave.empty"));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MicrowaveOvenBlockEntity be) {
            data.putInt(KEY_COOK_PROGRESS, be.getCookProgress());
            data.putBoolean(KEY_COOKING, be.isCooking());
            data.putBoolean(KEY_HAS_FOOD, !be.getFood().isEmpty());
        }
    }

    @Override
    public net.minecraft.resources.ResourceLocation getUid() {
        return XiaoshisJadePlugin.MICROWAVE_COOK_PROGRESS;
    }
}