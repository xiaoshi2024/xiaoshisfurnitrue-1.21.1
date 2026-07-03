package com.xiaoshi2022.xiaoshisfurnitrue.block.entity;

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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

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

    private final FluidTank tank = new FluidTank(3000) {
        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            updateStateFromTank();
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    /**
     * 从水桶加满（一桶 = 3级 = 3000mB）
     */
    public void fillFullBucket() {
        if (level == null) return;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);

        // 如果已经有水，先清空再设置
        if (currentLevel > 0) {
            // 清空现有液体
            tank.drain(tank.getFluidInTank(0).getAmount(), FluidAction.EXECUTE);
        }

        // 加满 3000mB
        FluidStack water = new FluidStack(
                net.minecraft.world.level.material.Fluids.WATER,
                3000
        );
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

    /**
     * 从水桶填充（由 Block 调用）
     */
    public void fillFromBucket() {
        if (level == null) return;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);

        // 每级对应 1000mB
        int currentAmount = currentLevel * 1000;
        int maxFill = 3000 - currentAmount;

        if (maxFill > 0) {
            // 模拟水
            FluidStack water = new FluidStack(
                    net.minecraft.world.level.material.Fluids.WATER,
                    Math.min(maxFill, 1000)
            );
            tank.fill(water, FluidAction.EXECUTE);
            updateStateFromTank();
        }
    }

    public InteractionResult dispenseWaterToBottle(Player player, ItemStack heldItem) {
        if (level == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);
        int temperature = state.getValue(WaterDispenserBlock.TEMPERATURE);

        if (currentLevel <= 0) {
            player.displayClientMessage(
                    Component.literal("§c饮水机是空的！"),
                    true
            );
            return InteractionResult.PASS;
        }

        if (!heldItem.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.PASS;
        }

        boolean isHot = temperature >= 2;
        ItemStack result = getBottleOutput();

        if (result.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("§e无法接取液体！"),
                    true
            );
            return InteractionResult.PASS;
        }

        if (!player.isCreative()) {
            heldItem.shrink(1);
            player.getInventory().add(result);
        }

        int newLevel = Math.max(currentLevel - 1, 0);
        boolean hasWater = newLevel > 0;

        BlockState newState = state
                .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                .setValue(WaterDispenserBlock.HAS_WATER, hasWater)
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
                Component.literal(
                        String.format("§a已接取 %s §a！(%d/%d)", liquidName, newLevel, WaterDispenserBlock.MAX_LEVEL)
                ),
                true
        );

        setChanged();
        level.sendBlockUpdated(worldPosition, state, newState, 3);

        // 从 FluidTank 中取出液体
        tank.drain(1000, FluidAction.EXECUTE);

        return InteractionResult.CONSUME;
    }

    private ItemStack getBottleOutput() {
        if (isToughAsNailsLoaded()) {
            ItemStack purifiedWater = getToughAsNailsPurifiedWater();
            if (!purifiedWater.isEmpty()) {
                return purifiedWater;
            }
        }

        ItemStack waterBottle = new ItemStack(Items.POTION);
        waterBottle.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        waterBottle.set(DataComponents.CUSTOM_NAME, Component.literal("§b水瓶"));
        return waterBottle;
    }

    private boolean isToughAsNailsLoaded() {
        return ModList.get().isLoaded(TOUGH_AS_NAILS_MODID);
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
        } catch (Exception e) {
            // 忽略
        }
        return ItemStack.EMPTY;
    }

    // ===== IFluidHandler 实现 =====

    @Override
    public int getTanks() {
        return tank.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tankIndex) {
        return this.tank.getFluidInTank(tankIndex);
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        return this.tank.getTankCapacity(tankIndex);
    }

    @Override
    public boolean isFluidValid(int tankIndex, FluidStack stack) {
        return this.tank.isFluidValid(tankIndex, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return tank.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return tank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(maxDrain, action);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        this.loadAdditional(tag, provider);
    }
}