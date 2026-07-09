package com.xiaoshi2022.xiaoshisfurnitrue.integration.jade;

import com.xiaoshi2022.xiaoshisfurnitrue.block.WaterDispenserBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WaterDispenserBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
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
        BlockEntity entity = accessor.getBlockEntity();

        int level = state.getValue(WaterDispenserBlock.WATER_LEVEL);
        boolean hasWater = state.getValue(WaterDispenserBlock.HAS_WATER);
        boolean heating = state.getValue(WaterDispenserBlock.HEATING);
        int temperature = state.getValue(WaterDispenserBlock.TEMPERATURE);

        IElementHelper helper = IElementHelper.get();

        // 1. 显示饮水机图标
        IElement icon = helper.item(getWaterIcon(state, entity), 0.75f)
                .size(new net.minecraft.world.phys.Vec2(14, 14))
                .translate(new net.minecraft.world.phys.Vec2(0, -2));
        icon.message(null);
        tooltip.add(icon);

        // 2. 显示水量
        Component waterLevelText = Component.translatable(
                "jade.xiaoshisfurnitrue.water_level",
                level,
                WaterDispenserBlock.MAX_LEVEL
        );
        tooltip.append(waterLevelText);

        // 3. 显示液体类型 (修复 getDisplayName 过时警告)
        if (hasWater && entity instanceof WaterDispenserBlockEntity dispenser) {
            FluidStack fluid = dispenser.getFluidInTank(0);
            if (!fluid.isEmpty()) {
                // 使用 getHoverName() 替代 getDisplayName()
                String fluidName = fluid.getHoverName().getString();
                Component fluidText = Component.literal(" §7(§r" + fluidName + "§7)");
                tooltip.append(fluidText);
            }
        }

        // 4. 显示温度状态
        if (hasWater) {
            Component tempText = getTemperatureText(temperature);
            tooltip.append(Component.literal(" "));
            tooltip.append(tempText);
        }

        // 5. 显示水质状态
        if (hasWater && entity instanceof WaterDispenserBlockEntity dispenser) {
            int purity = dispenser.getWaterPurity();
            Component purityText = getPurityText(purity);
            tooltip.append(Component.literal(" "));
            tooltip.append(purityText);
        }

        // 6. 显示加热状态
        if (heating && hasWater) {
            tooltip.append(Component.literal(" "));
            tooltip.append(HEATING_TEXT);
        } else if (heating && !hasWater) {
            tooltip.append(Component.literal(" "));
            tooltip.append(Component.literal("§6⚡ 加热中 (无水)"));
        }

        // 7. 显示水位条 (移除 Identifiers 依赖)
        // 直接显示进度条，不依赖 config 检查
        int filled = (int) ((level / (float) WaterDispenserBlock.MAX_LEVEL) * 100);
        String bar = getProgressBar(filled);
        tooltip.append(Component.literal(" "));
        tooltip.append(Component.literal(bar));

        // 7. 添加使用提示
        tooltip.append(Component.literal(" "));
        tooltip.append(Component.literal("§7[右键: 取水 | 空手: 加热]"));
    }

    /**
     * 获取温度显示文字
     */
    private Component getTemperatureText(int temperature) {
        return switch (temperature) {
            case 0 -> Component.translatable("jade.xiaoshisfurnitrue.temp.cold");
            case 1 -> Component.translatable("jade.xiaoshisfurnitrue.temp.warm");
            case 2 -> Component.translatable("jade.xiaoshisfurnitrue.temp.hot");
            case 3 -> Component.translatable("jade.xiaoshisfurnitrue.temp.boiling");
            default -> Component.literal("§7❓ 未知");
        };
    }

    /**
     * 获取水质显示文字
     */
    private Component getPurityText(int purity) {
        return switch (purity) {
            case 0 -> Component.literal("§4💀 脏水");
            case 1 -> Component.literal("§6⚠️ 微脏");
            case 2 -> Component.literal("§a✅ 可接受");
            case 3 -> Component.literal("§b✨ 纯净");
            default -> Component.literal("§7❓ 未知");
        };
    }

    /**
     * 获取进度条
     */
    private String getProgressBar(int percent) {
        int bars = 10;
        int filled = (int) (bars * percent / 100.0);
        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§b█");
            } else {
                sb.append("§7░");
            }
        }
        sb.append("§7]");
        return sb.toString();
    }

    /**
     * 获取显示的图标
     */
    private ItemStack getWaterIcon(BlockState state, BlockEntity entity) {
        int level = state.getValue(WaterDispenserBlock.WATER_LEVEL);
        int temperature = state.getValue(WaterDispenserBlock.TEMPERATURE);

        if (level == 0) {
            return new ItemStack(Items.BUCKET);
        }

        // 根据温度选择不同图标
        if (temperature >= 2) {
            return new ItemStack(Items.LAVA_BUCKET);
        }

        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    public net.minecraft.resources.ResourceLocation getUid() {
        return XiaoshisJadePlugin.WATER_LEVEL;
    }

    // ===== 颜色和文本常量 =====
    private static final Component HOT_TEXT = Component.literal("§c🔥 热水");
    private static final Component COLD_TEXT = Component.literal("§b❄️ 冷水");
    private static final Component HEATING_TEXT = Component.literal("§6⚡ 加热中");
    private static final Component IDLE_TEXT = Component.literal("§7💤 待机");
}