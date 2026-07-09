package com.xiaoshi2022.xiaoshisfurnitrue.block.entity;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import com.xiaoshi2022.xiaoshisfurnitrue.block.WaterDispenserBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class WaterDispenserBlockEntity extends BlockEntity implements GeoBlockEntity, IFluidHandler {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation USE_COLD = RawAnimation.begin().thenPlay("use_cold");
    private static final RawAnimation USE_HOT = RawAnimation.begin().thenPlay("use_hot");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlay("open");
    private static final RawAnimation FULL = RawAnimation.begin().thenPlay("full");
    private static final RawAnimation EMPTY = RawAnimation.begin().thenPlay("empty");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String TOUGH_AS_NAILS_MODID = "toughasnails";
    private static final String PURIFIED_WATER_PATH = "purified_water_bottle";
    private static final String THIRST_MODID = "thirst";
    private static final String THIRST_CANTEEN_MODID = "thirstcanteen";

    // ===== 水质常量 =====
    public static final int QUALITY_PURIFIED = 0;
    public static final int QUALITY_CLEAN = 1;
    public static final int QUALITY_WASTE = 2;
    public static final int QUALITY_POISONOUS = 3;

    // 当前水质纯度 (0-3)
    private int waterPurity = 2;

    private final FluidTank tank = new FluidTank(3000) {
        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            updateWaterPurityFromTank();
            updateStateFromTank();
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return isFluidAcceptable(stack);
        }
    };

    private boolean isFluidAcceptable(FluidStack stack) {
        if (stack.isEmpty()) return false;
        var fluid = stack.getFluid();
        var key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key == null) return false;

        String namespace = key.getNamespace();
        String path = key.getPath();

        if (fluid.is(FluidTags.WATER)) return true;
        if (namespace.equals("minecraft") && (path.contains("water") || path.contains("potion"))) return true;
        if (namespace.equals("toughasnails")) return true;
        if (namespace.equals("thirstcanteen")) return true;
        if (fluid.getFluidType().getDescriptionId().contains("water")) return true;

        String fluidName = fluid.getFluidType().getDescription().getString().toLowerCase();
        return fluidName.contains("污水") || fluidName.contains("waste") ||
                fluidName.contains("脏水") || fluidName.contains("dirty") ||
                fluidName.contains("受污染") || fluidName.contains("contaminated") ||
                fluidName.contains("毒水") || fluidName.contains("poison");
    }

    // ===== 水质管理 =====

    private void updateWaterPurityFromTank() {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return;
        }

        // ===== 只从 tank 读取，不修改 =====
        if (isThirstLoaded()) {
            try {
                Integer purity = getPurityFromFluid(fluid);
                if (purity != null) {
                    waterPurity = Math.max(0, Math.min(3, purity));
                    return;
                }
            } catch (Exception ignored) {}
        }

        String fluidName = fluid.getFluid().getFluidType().getDescription().getString().toLowerCase();
        if (fluidName.contains("污水") || fluidName.contains("waste") || fluidName.contains("脏") || fluidName.contains("dirty")) {
            waterPurity = 0;
        } else if (fluidName.contains("净化") || fluidName.contains("purified") || fluidName.contains("纯净") || fluidName.contains("pure")) {
            waterPurity = 3;
        } else {
            waterPurity = 2;
        }
    }

    public int getWaterPurity() {
        return waterPurity;
    }

    public void setWaterPurity(int purity) {
        this.waterPurity = Math.max(0, Math.min(3, purity));
        updateFluidPurity();
    }

    private void updateFluidPurity() {
        FluidStack fluid = tank.getFluid();
        if (!fluid.isEmpty() && isThirstLoaded()) {
            setPurityToFluid(fluid, waterPurity);
        }
    }

    public boolean isSafeToDrink() {
        return waterPurity >= 2;
    }

    // ===== 加热净化逻辑 =====
    public void onHeatingComplete() {
        if (level == null) return;
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) return;

        // ===== 加热只提升到可接受级别 =====
        if (waterPurity < 2) {
            waterPurity = 2;
        } else if (waterPurity >= 1) {
            return;
        }

        if (isThirstLoaded()) {
            setPurityToFluid(fluid, waterPurity);
        }

        updateStateFromTank();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ===== 加水逻辑 =====

    public void fillFullBucket(ItemStack bucketStack) {
        if (level == null) return;

        tank.drain(tank.getFluidInTank(0).getAmount(), FluidAction.EXECUTE);

        FluidStack water = new FluidStack(
                net.minecraft.world.level.material.Fluids.WATER,
                3000
        );

        int purity = 2;
        if (isThirstLoaded() && bucketStack != null) {
            Integer purityFromItem = getPurityFromItem(bucketStack);
            if (purityFromItem != null) {
                purity = Math.max(0, Math.min(3, purityFromItem));
            }
        }

        if (isThirstLoaded()) {
            setPurityToFluid(water, purity);
        }

        waterPurity = purity;
        tank.fill(water, FluidAction.EXECUTE);
        updateStateFromTank();
    }

    public void fillFromBucket(ItemStack bucketStack) {
        if (level == null) return;

        int currentAmount = tank.getFluidAmount();
        int maxFill = 3000 - currentAmount;
        if (maxFill <= 0) return;

        FluidStack water = new FluidStack(
                net.minecraft.world.level.material.Fluids.WATER,
                Math.min(maxFill, 1000)
        );

        int purity = 2;
        if (isThirstLoaded() && bucketStack != null) {
            Integer purityFromItem = getPurityFromItem(bucketStack);
            if (purityFromItem != null) {
                purity = Math.max(0, Math.min(3, purityFromItem));
            }
        }

        if (isThirstLoaded()) {
            setPurityToFluid(water, purity);
        }

        if (tank.getFluidAmount() > 0) {
            int currentPurity = waterPurity;
            int newPurity = (currentPurity * tank.getFluidAmount() + purity * water.getAmount()) /
                    (tank.getFluidAmount() + water.getAmount());
            waterPurity = Math.max(0, Math.min(3, newPurity));
        } else {
            waterPurity = purity;
        }

        tank.fill(water, FluidAction.EXECUTE);
        updateStateFromTank();
    }

    public WaterDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_DISPENSER_BLOCK_ENTITY.get(), pos, state);
    }

    public void updateStateFromTank() {
        if (level == null) return;

        BlockState state = level.getBlockState(worldPosition);
        FluidStack fluid = tank.getFluid();

        int amount = fluid.getAmount();
        int levelValue = Math.min(amount / 1000, WaterDispenserBlock.MAX_LEVEL);
        boolean hasWater = amount > 0;

        BlockState newState = state
                .setValue(WaterDispenserBlock.WATER_LEVEL, levelValue)
                .setValue(WaterDispenserBlock.HAS_WATER, hasWater);

        if (!hasWater) {
            newState = newState
                    .setValue(WaterDispenserBlock.HEATING, false)
                    .setValue(WaterDispenserBlock.TEMPERATURE, 0);
        }

        level.setBlock(worldPosition, newState, 3);
        setChanged();
    }

    // ===== 接水逻辑 =====

    public InteractionResult dispenseWaterToBottle(Player player, ItemStack heldItem) {
        if (level == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);
        int temperature = state.getValue(WaterDispenserBlock.TEMPERATURE);

        if (currentLevel <= 0) {
            player.displayClientMessage(Component.literal("§c饮水机是空的！"), true);
            return InteractionResult.PASS;
        }

        if (!heldItem.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.PASS;
        }

        if (waterPurity < 2) {
            player.displayClientMessage(Component.literal("§c饮水机里的水是污水/脏水，不能装瓶！"), true);
            return InteractionResult.PASS;
        }

        boolean isHot = temperature >= 2;
        ItemStack result = getBottleOutput(waterPurity);

        if (result.isEmpty()) {
            player.displayClientMessage(Component.literal("§e无法接取液体！"), true);
            return InteractionResult.PASS;
        }

        if (!player.isCreative()) {
            heldItem.shrink(1);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }

        int newLevel = Math.max(currentLevel - 1, 0);
        tank.drain(1000, FluidAction.EXECUTE);

        BlockState newState = state
                .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                .setValue(WaterDispenserBlock.HAS_WATER, newLevel > 0)
                .setValue(WaterDispenserBlock.DISPENSING, true);

        if (newLevel == 0) {
            newState = newState
                    .setValue(WaterDispenserBlock.HEATING, false)
                    .setValue(WaterDispenserBlock.TEMPERATURE, 0);
        }

        level.setBlock(worldPosition, newState, 3);
        level.scheduleTick(worldPosition, getBlockState().getBlock(), WaterDispenserBlock.DISPENSING_ANIM_TICKS);

        if (isHot) {
            level.playSound(null, worldPosition, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.0f);
        } else {
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5f, 1.0f);
        }

        String liquidName = isHot ? "热水" : "水";
        player.displayClientMessage(
                Component.literal(String.format("§a已接取 %s §a！(%d/%d)", liquidName, newLevel, WaterDispenserBlock.MAX_LEVEL)),
                true
        );

        updateWaterPurityFromTank();
        setChanged();
        level.sendBlockUpdated(worldPosition, state, newState, 3);

        return InteractionResult.CONSUME;
    }

    private ItemStack getBottleOutput(int purity) {
        ItemStack bottle;

        if (isToughAsNailsLoaded() && purity >= 3) {
            ItemStack purifiedBottle = getToughAsNailsPurifiedWater();
            if (!purifiedBottle.isEmpty()) {
                bottle = purifiedBottle;
                if (isThirstLoaded()) {
                    setPurityToItem(bottle, purity);
                }
                return bottle;
            }
        }

        bottle = new ItemStack(Items.POTION);
        bottle.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));

        if (isThirstLoaded()) {
            setPurityToItem(bottle, purity);
        }

        if (purity >= 3) {
            bottle.set(DataComponents.CUSTOM_NAME, Component.literal("§b净化水瓶"));
        } else if (purity >= 2) {
            bottle.set(DataComponents.CUSTOM_NAME, Component.literal("§a水瓶"));
        }

        return bottle;
    }

    // ===== 容器交互（软联动 - 使用反射） =====

    public InteractionResult tryDispenseToContainer(Player player, ItemStack heldItem, Level level, BlockPos pos) {
        if (level == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);

        if (currentLevel <= 0) return InteractionResult.PASS;
        if (tank.getFluidAmount() < 1000) return InteractionResult.PASS;

        if (isThirstLoaded()) {
            try {
                if (isEmptyWaterContainer(heldItem)) {
                    ItemStack filledContainer = getFilledContainer(heldItem, true);
                    if (!filledContainer.isEmpty()) {
                        setPurityToItem(filledContainer, waterPurity);
                        return giveFilledContainerToPlayer(player, heldItem, filledContainer, state);
                    }
                }
            } catch (Exception ignored) {}
        }

        // ===== 2. 通过反射处理 Thirst Canteen 水壶 =====
        if (ModList.get().isLoaded(THIRST_CANTEEN_MODID)) {
            InteractionResult result = handleCanteenByReflection(player, heldItem, state);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleCanteenByReflection(Player player, ItemStack heldItem, BlockState state) {
        try {
            Class<?> emptyCanteenClass = Class.forName("vip.fubuki.thirstcanteen.common.item.EmptyCanteen");
            if (emptyCanteenClass.isInstance(heldItem.getItem())) {
                return handleEmptyCanteenReflection(player, heldItem, state, emptyCanteenClass);
            }

            Class<?> canteenClass = Class.forName("vip.fubuki.thirstcanteen.common.item.Canteen");
            if (canteenClass.isInstance(heldItem.getItem())) {
                return handleFullCanteenReflection(player, heldItem, state, canteenClass);
            }
        } catch (ClassNotFoundException ignored) {}
        catch (Exception ignored) {}
        return InteractionResult.PASS;
    }

    private InteractionResult handleEmptyCanteenReflection(Player player, ItemStack heldItem,
                                                           BlockState state, Class<?> emptyCanteenClass) {
        try {
            Field containerField = emptyCanteenClass.getDeclaredField("container");
            containerField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Supplier<ItemStack> containerSupplier = (Supplier<ItemStack>) containerField.get(heldItem.getItem());
            ItemStack result = containerSupplier.get().copy();

            Class<?> canteenClass = Class.forName("vip.fubuki.thirstcanteen.common.item.Canteen");
            if (!canteenClass.isInstance(result.getItem())) {
                return InteractionResult.PASS;
            }

            Method getMaxUsableTimes = canteenClass.getMethod("getMaxUsableTimes");
            int maxUses = (int) getMaxUsableTimes.invoke(result.getItem());

            // ===== 计算需要多少水 =====
            int totalNeededWater = maxUses * 250;

            int currentWater = tank.getFluidAmount();
            if (currentWater <= 0) {
                player.displayClientMessage(Component.literal("§c饮水机是空的！"), true);
                return InteractionResult.PASS;
            }

            // ===== 计算能装多少 =====
            int canFillUses = Math.min(maxUses, currentWater / 250);
            if (canFillUses <= 0) {
                player.displayClientMessage(Component.literal("§c水量不足！至少需要 250mB"), true);
                return InteractionResult.PASS;
            }

            int waterToTake = canFillUses * 250;
            int savedPurity = waterPurity;

            // ===== 舀水 =====
            if (waterToTake >= currentWater) {
                tank.drain(currentWater, FluidAction.EXECUTE);
            } else {
                tank.drain(waterToTake, FluidAction.EXECUTE);
            }

            // ===== 恢复水质 =====
            waterPurity = savedPurity;

            if (tank.getFluidAmount() > 0 && isThirstLoaded()) {
                FluidStack remaining = tank.getFluidInTank(0);
                if (!remaining.isEmpty()) {
                    setPurityToFluid(remaining, savedPurity);
                }
            }

            // ===== 设置水壶的充能次数 =====
            Method setContain = canteenClass.getMethod("setContain", ItemStack.class, int.class);
            setContain.invoke(null, result, canFillUses);

            // ===== 设置水壶的纯度 =====
            if (isThirstLoaded()) {
                setPurityToItem(result, savedPurity);
            }

            // ===== 给玩家水壶 =====
            if (!player.isCreative()) {
                heldItem.shrink(1);
                if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }
            }

            // ===== 更新饮水机状态 =====
            int newLevel = tank.getFluidAmount() / 1000;
            BlockState newState = state
                    .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                    .setValue(WaterDispenserBlock.HAS_WATER, newLevel > 0)
                    .setValue(WaterDispenserBlock.DISPENSING, true);

            if (newLevel == 0) {
                newState = newState
                        .setValue(WaterDispenserBlock.HEATING, false)
                        .setValue(WaterDispenserBlock.TEMPERATURE, 0);
            }

            level.setBlock(worldPosition, newState, 3);
            level.scheduleTick(worldPosition, getBlockState().getBlock(), WaterDispenserBlock.DISPENSING_ANIM_TICKS);
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5f, 1.0f);

            // ===== 简单提示 =====
            player.displayClientMessage(
                    Component.literal(String.format("§a已装取水壶！(%d/%d 次)", canFillUses, maxUses)),
                    true
            );

            updateWaterPurityFromTank();
            setChanged();
            level.sendBlockUpdated(worldPosition, state, newState, 3);

            return InteractionResult.CONSUME;

        } catch (Exception e) {
            XiaoshisFurnitrue.LOGGER.error("反射处理空水壶失败: {}", e.getMessage(), e);
            return InteractionResult.PASS;
        }
    }

    private InteractionResult handleFullCanteenReflection(Player player, ItemStack heldItem,
                                                          BlockState state, Class<?> canteenClass) {
        try {
            Method getMaxUsableTimes = canteenClass.getMethod("getMaxUsableTimes");
            int maxUses = (int) getMaxUsableTimes.invoke(heldItem.getItem());

            Method getLeftUsableTimes = canteenClass.getMethod("getLeftUsableTimes", ItemStack.class);
            int leftUses = (int) getLeftUsableTimes.invoke(heldItem.getItem(), heldItem);

            if (leftUses >= maxUses) {
                player.displayClientMessage(Component.literal("§e水壶已经满了！"), true);
                return InteractionResult.PASS;
            }

            int missingUses = maxUses - leftUses;
            int neededWater = missingUses * 250;

            int currentWater = tank.getFluidAmount();
            if (currentWater <= 0) {
                player.displayClientMessage(Component.literal("§c饮水机是空的！"), true);
                return InteractionResult.PASS;
            }

            int canFillUses = Math.min(missingUses, currentWater / 250);
            if (canFillUses <= 0) {
                player.displayClientMessage(Component.literal("§c水量不足！至少需要 250mB"), true);
                return InteractionResult.PASS;
            }

            int waterToTake = canFillUses * 250;
            int savedPurity = waterPurity;

            // ===== 舀水 =====
            if (waterToTake >= currentWater) {
                tank.drain(currentWater, FluidAction.EXECUTE);
            } else {
                tank.drain(waterToTake, FluidAction.EXECUTE);
            }

            // ===== 恢复水质 =====
            waterPurity = savedPurity;

            if (tank.getFluidAmount() > 0 && isThirstLoaded()) {
                FluidStack remaining = tank.getFluidInTank(0);
                if (!remaining.isEmpty()) {
                    setPurityToFluid(remaining, savedPurity);
                }
            }

            // ===== 更新水壶 =====
            Method setContain = canteenClass.getMethod("setContain", ItemStack.class, int.class);
            setContain.invoke(null, heldItem, leftUses + canFillUses);

            // ===== 设置水壶的纯度 =====
            if (isThirstLoaded()) {
                setPurityToItem(heldItem, savedPurity);
            }

            // ===== 更新饮水机状态 =====
            int newLevel = tank.getFluidAmount() / 1000;
            BlockState newState = state
                    .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                    .setValue(WaterDispenserBlock.HAS_WATER, newLevel > 0)
                    .setValue(WaterDispenserBlock.DISPENSING, true);

            if (newLevel == 0) {
                newState = newState
                        .setValue(WaterDispenserBlock.HEATING, false)
                        .setValue(WaterDispenserBlock.TEMPERATURE, 0);
            }

            level.setBlock(worldPosition, newState, 3);
            level.scheduleTick(worldPosition, getBlockState().getBlock(), WaterDispenserBlock.DISPENSING_ANIM_TICKS);
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5f, 1.0f);

            // ===== 简单提示 =====
            player.displayClientMessage(
                    Component.literal(String.format("§a已补充水壶！(%d/%d 次)", leftUses + canFillUses, maxUses)),
                    true
            );

            updateWaterPurityFromTank();
            setChanged();
            level.sendBlockUpdated(worldPosition, state, newState, 3);

            return InteractionResult.CONSUME;

        } catch (Exception e) {
            XiaoshisFurnitrue.LOGGER.error("反射处理满水壶失败: {}", e.getMessage(), e);
            return InteractionResult.PASS;
        }
    }

    private InteractionResult giveFilledContainerToPlayer(Player player, ItemStack heldItem,
                                                          ItemStack filledContainer, BlockState state) {
        if (!player.isCreative()) {
            heldItem.shrink(1);
            if (!player.getInventory().add(filledContainer)) {
                player.drop(filledContainer, false);
            }
        }

        int newLevel = Math.max(state.getValue(WaterDispenserBlock.WATER_LEVEL) - 1, 0);
        tank.drain(1000, FluidAction.EXECUTE);

        BlockState newState = state
                .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                .setValue(WaterDispenserBlock.HAS_WATER, newLevel > 0)
                .setValue(WaterDispenserBlock.DISPENSING, true);

        if (newLevel == 0) {
            newState = newState
                    .setValue(WaterDispenserBlock.HEATING, false)
                    .setValue(WaterDispenserBlock.TEMPERATURE, 0);
        }

        level.setBlock(worldPosition, newState, 3);
        level.scheduleTick(worldPosition, getBlockState().getBlock(), WaterDispenserBlock.DISPENSING_ANIM_TICKS);
        level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5f, 1.0f);

        player.displayClientMessage(
                Component.literal(String.format("§a已装水！(%d/%d)", newLevel, WaterDispenserBlock.MAX_LEVEL)),
                true
        );

        updateWaterPurityFromTank();
        setChanged();
        level.sendBlockUpdated(worldPosition, state, newState, 3);

        return InteractionResult.CONSUME;
    }

    // ===== 辅助方法 =====

    private boolean isToughAsNailsLoaded() {
        return ModList.get().isLoaded(TOUGH_AS_NAILS_MODID);
    }

    private boolean isThirstLoaded() {
        return ModList.get().isLoaded(THIRST_MODID);
    }

    private Integer getPurityFromFluid(FluidStack fluid) {
        if (!isThirstLoaded() || fluid.isEmpty()) return null;
        try {
            Class<?> thirstComponentClass = Class.forName("dev.ghen.thirst.content.registry.ThirstComponent");
            Field purityField = thirstComponentClass.getDeclaredField("PURITY");
            purityField.setAccessible(true);
            Object purityComponent = purityField.get(null);
            return fluid.get((net.minecraft.core.component.DataComponentType<Integer>) purityComponent);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setPurityToFluid(FluidStack fluid, int purity) {
        if (!isThirstLoaded() || fluid.isEmpty()) return;
        try {
            Class<?> thirstComponentClass = Class.forName("dev.ghen.thirst.content.registry.ThirstComponent");
            Field purityField = thirstComponentClass.getDeclaredField("PURITY");
            purityField.setAccessible(true);
            Object purityComponent = purityField.get(null);
            fluid.set((net.minecraft.core.component.DataComponentType<Integer>) purityComponent, purity);
        } catch (Exception ignored) {}
    }

    private Integer getPurityFromItem(ItemStack stack) {
        if (!isThirstLoaded() || stack.isEmpty()) return null;
        try {
            Class<?> waterPurityClass = Class.forName("dev.ghen.thirst.content.purity.WaterPurity");
            Method getPurityMethod = waterPurityClass.getDeclaredMethod("getPurity", ItemStack.class);
            getPurityMethod.setAccessible(true);
            return (Integer) getPurityMethod.invoke(null, stack);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setPurityToItem(ItemStack stack, int purity) {
        if (!isThirstLoaded() || stack.isEmpty()) return;
        try {
            Class<?> thirstComponentClass = Class.forName("dev.ghen.thirst.content.registry.ThirstComponent");
            Field purityField = thirstComponentClass.getDeclaredField("PURITY");
            purityField.setAccessible(true);
            Object purityComponent = purityField.get(null);
            stack.set((net.minecraft.core.component.DataComponentType<Integer>) purityComponent, purity);
        } catch (Exception ignored) {}
    }

    private boolean isEmptyWaterContainer(ItemStack stack) {
        if (!isThirstLoaded() || stack.isEmpty()) return false;
        try {
            Class<?> waterPurityClass = Class.forName("dev.ghen.thirst.content.purity.WaterPurity");
            Method isEmptyContainerMethod = waterPurityClass.getDeclaredMethod("isEmptyWaterContainer", ItemStack.class);
            isEmptyContainerMethod.setAccessible(true);
            return (boolean) isEmptyContainerMethod.invoke(null, stack);
        } catch (Exception ignored) {
            return false;
        }
    }

    private ItemStack getFilledContainer(ItemStack stack, boolean copy) {
        if (!isThirstLoaded() || stack.isEmpty()) return ItemStack.EMPTY;
        try {
            Class<?> waterPurityClass = Class.forName("dev.ghen.thirst.content.purity.WaterPurity");
            Method getFilledContainerMethod = waterPurityClass.getDeclaredMethod("getFilledContainer", ItemStack.class, boolean.class);
            getFilledContainerMethod.setAccessible(true);
            return (ItemStack) getFilledContainerMethod.invoke(null, stack, copy);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack getToughAsNailsPurifiedWater() {
        try {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    TOUGH_AS_NAILS_MODID,
                    PURIFIED_WATER_PATH
            );
            var item = BuiltInRegistries.ITEM.get(location);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    // ===== IFluidHandler 实现 =====

    @Override
    public int getTanks() {
        return tank.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tankIndex) {
        FluidStack fluid = this.tank.getFluidInTank(tankIndex);
        if (fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack waterStack = new FluidStack(
                net.minecraft.world.level.material.Fluids.WATER,
                fluid.getAmount()
        );

        // ===== 直接使用 waterPurity，不从 tank 读取 =====
        if (isThirstLoaded()) {
            setPurityToFluid(waterStack, waterPurity);
        }

        return waterStack;
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        return this.tank.getTankCapacity(tankIndex);
    }

    @Override
    public boolean isFluidValid(int tankIndex, FluidStack stack) {
        return isFluidAcceptable(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidAcceptable(resource)) {
            return 0;
        }

        int filled = tank.fill(resource, action);
        if (action.execute() && filled > 0) {
            updateWaterPurityFromTank();
            updateStateFromTank();
            setChanged();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack drained = tank.drain(resource, action);
        if (action.execute() && !drained.isEmpty()) {
            FluidStack waterStack = new FluidStack(
                    net.minecraft.world.level.material.Fluids.WATER,
                    drained.getAmount()
            );
            if (isThirstLoaded()) {
                Integer purity = getPurityFromFluid(drained);
                if (purity != null) {
                    setPurityToFluid(waterStack, purity);
                } else {
                    setPurityToFluid(waterStack, waterPurity);
                }
            }
            drained = waterStack;
            updateStateFromTank();
            setChanged();
        }
        return drained;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // 先执行 drain
        FluidStack drained = tank.drain(maxDrain, action);
        if (action.execute() && !drained.isEmpty()) {
            // 创建全新的 FluidStack，完全独立
            FluidStack waterStack = new FluidStack(
                    net.minecraft.world.level.material.Fluids.WATER,
                    drained.getAmount()
            );

            if (isThirstLoaded()) {
                setPurityToFluid(waterStack, waterPurity);
            }

            // 更新 tank 状态
            updateStateFromTank();
            setChanged();

            // 返回新的 FluidStack，不返回 tank.drain 的直接结果
            return waterStack;
        }
        return drained;
    }

    // ===== GeckoLib 动画 =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<WaterDispenserBlockEntity> mainController = new AnimationController<>(
                this, "main_controller", 0, state -> {
            BlockState blockState = getBlockState();
            if (blockState.getValue(WaterDispenserBlock.DISPENSING)) {
                int temp = blockState.getValue(WaterDispenserBlock.TEMPERATURE);
                if (temp >= 2) {
                    return state.setAndContinue(USE_HOT);
                }
                return state.setAndContinue(USE_COLD);
            }
            return state.setAndContinue(IDLE);
        });

        AnimationController<WaterDispenserBlockEntity> waterController = new AnimationController<>(
                this, "water_controller", 0, state -> {
            BlockState blockState = getBlockState();
            int waterLvl = blockState.getValue(WaterDispenserBlock.WATER_LEVEL);
            if (waterLvl == 0) {
                return state.setAndContinue(EMPTY);
            } else if (waterLvl >= WaterDispenserBlock.MAX_LEVEL) {
                return state.setAndContinue(FULL);
            }
            return state.setAndContinue(IDLE);
        });

        AnimationController<WaterDispenserBlockEntity> doorController = new AnimationController<>(
                this, "door_controller", 0, state -> {
            BlockState blockState = getBlockState();
            if (blockState.getValue(WaterDispenserBlock.OPEN)) {
                return state.setAndContinue(OPEN);
            }
            return state.setAndContinue(IDLE);
        });

        controllers.add(mainController);
        controllers.add(waterController);
        controllers.add(doorController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ===== NBT 持久化 =====

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("waterPurity", waterPurity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        if (tag.contains("waterPurity")) {
            waterPurity = tag.getInt("waterPurity");
        }
        updateWaterPurityFromTank();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        this.loadAdditional(tag, provider);
    }
}