package com.xiaoshi2022.xiaoshisfurnitrue.block.entity;

import com.xiaoshi2022.xiaoshisfurnitrue.block.WaterDispenserBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WaterDispenserBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation USE_COLD = RawAnimation.begin().thenPlay("use_cold");
    private static final RawAnimation USE_HOT = RawAnimation.begin().thenPlay("use_hot");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlay("open");
    private static final RawAnimation FULL = RawAnimation.begin().thenPlay("full");
    private static final RawAnimation EMPTY = RawAnimation.begin().thenPlay("empty");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean isUsing = false;
    private boolean isOpen = false;

    public WaterDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_DISPENSER_BLOCK_ENTITY.get(), pos, state);
    }

    public InteractionResult fillWater(Player player, ItemStack heldItem) {
        if (level == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);

        if (currentLevel >= 3) {
            // 已满
            return InteractionResult.PASS;
        }

        // 消耗一个物品，增加水量
        if (!player.isCreative()) {
            heldItem.shrink(1);
            // 返还空容器
            if (heldItem.getItem() == Items.WATER_BUCKET) {
                player.getInventory().add(new ItemStack(Items.BUCKET));
            } else if (heldItem.getItem() == Items.GLASS_BOTTLE) {
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        int newLevel = Math.min(currentLevel + 1, 3);
        boolean hasWater = newLevel > 0;

        level.setBlock(worldPosition, state
                .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                .setValue(WaterDispenserBlock.HAS_WATER, hasWater), 3);

        // 播放满水动画
        if (newLevel == 3) {
            triggerAnim("water_controller", "full");
        }

        level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5f, 1.0f);

        // 同步到客户端
        setChanged();
        level.sendBlockUpdated(worldPosition, state, state, 3);

        return InteractionResult.CONSUME;
    }

    public InteractionResult dispenseWater(Player player, BlockHitResult hit) {
        if (level == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);

        if (currentLevel <= 0) {
            return InteractionResult.PASS;
        }

        // 检查玩家是否拿着玻璃瓶
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldItem.getItem() != Items.GLASS_BOTTLE) {
            // 没拿瓶子，提示或忽略
            return InteractionResult.PASS;
        }

        // 消耗一个瓶子，给水瓶
        if (!player.isCreative()) {
            heldItem.shrink(1);
            player.getInventory().add(new ItemStack(Items.POTION));
        }

        // 减少水量
        int newLevel = Math.max(currentLevel - 1, 0);
        boolean hasWater = newLevel > 0;

        level.setBlock(worldPosition, state
                .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                .setValue(WaterDispenserBlock.HAS_WATER, hasWater), 3);

        // 播放取水动画
        triggerAnim("water_controller", "use_cold");

        // 如果空了，播放空动画
        if (newLevel == 0) {
            triggerAnim("water_controller", "empty");
        }

        level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5f, 1.0f);

        // 同步到客户端
        setChanged();
        level.sendBlockUpdated(worldPosition, state, state, 3);

        return InteractionResult.CONSUME;
    }

    public void triggerAnim(String controller, String animName) {
        // 通过 AnimationController 触发动画
        // 实际触发在 registerControllers 中处理
        this.isUsing = true;
        this.setChanged();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主控制器：空闲 + 取水
        AnimationController<WaterDispenserBlockEntity> mainController = new AnimationController<>(
                this, "main_controller", 0, state -> {
            if (isUsing) {
                // 播放取水动画后回到空闲
                isUsing = false;
                return state.setAndContinue(USE_COLD);
            }
            return state.setAndContinue(IDLE);
        }
        );

        // 水控制器：满/空动画
        AnimationController<WaterDispenserBlockEntity> waterController = new AnimationController<>(
                this, "water_controller", 0, state -> {
            BlockState blockState = level.getBlockEntity(worldPosition) != null ?
                    level.getBlockState(worldPosition) : null;
            if (blockState != null) {
                boolean hasWater = blockState.getValue(WaterDispenserBlock.HAS_WATER);
                int level = blockState.getValue(WaterDispenserBlock.WATER_LEVEL);
                if (!hasWater || level == 0) {
                    return state.setAndContinue(EMPTY);
                } else if (level == 3) {
                    return state.setAndContinue(FULL);
                }
            }
            return state.setAndContinue(IDLE);
        }
        );

        // 开门控制器
        AnimationController<WaterDispenserBlockEntity> doorController = new AnimationController<>(
                this, "door_controller", 0, state -> {
            if (isOpen) {
                return state.setAndContinue(OPEN);
            }
            return state.setAndContinue(IDLE);
        }
        );

        controllers.add(mainController);
        controllers.add(waterController);
        controllers.add(doorController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
        this.setChanged();
    }
}