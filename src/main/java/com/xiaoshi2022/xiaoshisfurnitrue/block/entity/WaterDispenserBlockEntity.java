package com.xiaoshi2022.xiaoshisfurnitrue.block.entity;

import com.xiaoshi2022.xiaoshisfurnitrue.block.WaterDispenserBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import org.jetbrains.annotations.Nullable;
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

    public WaterDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_DISPENSER_BLOCK_ENTITY.get(), pos, state);
    }

    public InteractionResult fillWater(Player player, ItemStack heldItem) {
        if (level == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);

        if (currentLevel >= 3) {
            return InteractionResult.PASS;
        }

        if (!player.isCreative()) {
            heldItem.shrink(1);
            player.getInventory().add(new ItemStack(Items.BUCKET));
        }

        int newLevel = Math.min(currentLevel + 1, 3);
        boolean hasWater = newLevel > 0;

        BlockState newState = state
                .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                .setValue(WaterDispenserBlock.HAS_WATER, hasWater);
        level.setBlock(worldPosition, newState, 3);

        level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5f, 1.0f);

        setChanged();
        level.sendBlockUpdated(worldPosition, state, newState, 3);

        return InteractionResult.CONSUME;
    }

    public InteractionResult dispenseWater(Player player, BlockHitResult hit) {
        if (level == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(worldPosition);
        int currentLevel = state.getValue(WaterDispenserBlock.WATER_LEVEL);

        if (currentLevel <= 0) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldItem.getItem() != Items.GLASS_BOTTLE) {
            return InteractionResult.PASS;
        }

        if (!player.isCreative()) {
            heldItem.shrink(1);
            player.getInventory().add(new ItemStack(Items.POTION));
        }

        int newLevel = Math.max(currentLevel - 1, 0);
        boolean hasWater = newLevel > 0;

        BlockState newState = state
                .setValue(WaterDispenserBlock.WATER_LEVEL, newLevel)
                .setValue(WaterDispenserBlock.HAS_WATER, hasWater)
                .setValue(WaterDispenserBlock.DISPENSING, true);
        level.setBlock(worldPosition, newState, 3);

        level.scheduleTick(worldPosition, getBlockState().getBlock(), WaterDispenserBlock.DISPENSING_ANIM_TICKS);

        level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5f, 1.0f);

        setChanged();
        level.sendBlockUpdated(worldPosition, state, newState, 3);

        return InteractionResult.CONSUME;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<WaterDispenserBlockEntity> mainController = new AnimationController<>(
                this, "main_controller", 0, state -> {
            BlockState blockState = getBlockState();
            if (blockState.getValue(WaterDispenserBlock.DISPENSING)) {
                return state.setAndContinue(USE_COLD);
            }
            return state.setAndContinue(IDLE);
        });

        AnimationController<WaterDispenserBlockEntity> waterController = new AnimationController<>(
                this, "water_controller", 0, state -> {
            BlockState blockState = getBlockState();
            boolean hasWater = blockState.getValue(WaterDispenserBlock.HAS_WATER);
            int waterLvl = blockState.getValue(WaterDispenserBlock.WATER_LEVEL);
            if (!hasWater || waterLvl == 0) {
                return state.setAndContinue(EMPTY);
            } else if (waterLvl == 3) {
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }
}
