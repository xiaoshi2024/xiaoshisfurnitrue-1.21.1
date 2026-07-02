package com.xiaoshi2022.xiaoshisfurnitrue.block.entity;

import com.xiaoshi2022.xiaoshisfurnitrue.block.MicrowaveOvenBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.network.MicrowaveOvenSyncPacket;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MicrowaveOvenBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation MAKING = RawAnimation.begin().thenLoop("making");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("open");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack food = ItemStack.EMPTY;
    private int cookTimer = 0;
    private int maxCookTime = 100;
    private boolean isCooking = false;

    public MicrowaveOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MICROWAVE_OVEN_BLOCK_ENTITY.get(), pos, state);
    }

    public void setFood(ItemStack stack) {
        this.food = stack.copy();
        this.setChanged();
        this.syncToClient();
    }

    public void setFoodClient(ItemStack stack) {
        this.food = stack.copy();
        this.setChanged();
    }

    public ItemStack getFood() {
        return food.copy();
    }

    public void startCooking() {
        if (food.isEmpty() || isCooking) return;
        this.isCooking = true;
        this.cookTimer = 0;
        this.maxCookTime = 100;
        this.setChanged();

        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.setBlock(worldPosition, state.setValue(MicrowaveOvenBlock.IS_COOKING, true), 3);
            level.playSound(null, worldPosition, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 0.3f, 1.0f);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MicrowaveOvenBlockEntity entity) {
        if (!entity.isCooking) return;

        entity.cookTimer++;

        if (entity.cookTimer >= entity.maxCookTime) {
            entity.finishCooking(level, pos, state);
        }

        if (entity.cookTimer % 20 == 0) {
            int progress = Math.min(entity.cookTimer / 5, 20);
            BlockState currentState = level.getBlockState(pos);
            level.setBlock(pos, currentState.setValue(MicrowaveOvenBlock.COOK_TIME, progress), 3);
        }
    }

    private void finishCooking(Level level, BlockPos pos, BlockState state) {
        this.isCooking = false;
        this.cookTimer = 0;

        if (!food.isEmpty()) {
            ItemStack cooked = getCookResult(level, food);
            if (!cooked.isEmpty()) {
                this.food = cooked.copy();
            }
        }

        BlockState currentState = level.getBlockState(pos);
        level.setBlock(pos, currentState
                .setValue(MicrowaveOvenBlock.IS_COOKING, false)
                .setValue(MicrowaveOvenBlock.COOK_TIME, 0), 3);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5f, 1.0f);

        this.setChanged();
        this.syncToClient();
    }

    private ItemStack getCookResult(Level level, ItemStack input) {
        if (level == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .orElse(null);

        if (recipe != null) {
            return recipe.value().getResultItem(level.registryAccess()).copy();
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主控制器 - 控制烹饪动画
        AnimationController<MicrowaveOvenBlockEntity> mainController = new AnimationController<>(
                this, "main_controller", 0, state -> {
            BlockState blockState = getBlockState();
            if (blockState.getValue(MicrowaveOvenBlock.IS_COOKING)) {
                return state.setAndContinue(MAKING);
            }
            return state.setAndContinue(IDLE);
        }
        );

        // 门控制器 - 直接根据 OPEN 状态播放
        AnimationController<MicrowaveOvenBlockEntity> doorController = new AnimationController<>(
                this, "door_controller", 0, state -> {
            BlockState blockState = getBlockState();
            if (blockState.getValue(MicrowaveOvenBlock.OPEN)) {
                return state.setAndContinue(OPEN);
            }
            return state.setAndContinue(IDLE);
        }
        );

        controllers.add(mainController);
        controllers.add(doorController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!food.isEmpty()) {
            tag.put("Food", food.save(provider, new CompoundTag()));
        }
        tag.putBoolean("Cooking", isCooking);
        tag.putInt("CookTimer", cookTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Food")) {
            food = ItemStack.parseOptional(provider, tag.getCompound("Food"));
        }
        isCooking = tag.getBoolean("Cooking");
        cookTimer = tag.getInt("CookTimer");
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

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T entity) {
        if (entity instanceof MicrowaveOvenBlockEntity oven) {
            MicrowaveOvenBlockEntity.tick(level, pos, state, oven);
        }
    }

    public boolean isCooking() {
        return isCooking;
    }

    public int getCookProgress() {
        return (int) ((float) cookTimer / maxCookTime * 100);
    }

    private void syncToClient() {
        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            this.setChanged();

            if (level instanceof ServerLevel serverLevel) {
                ChunkPos chunkPos = new ChunkPos(getBlockPos());
                PacketDistributor.sendToPlayersTrackingChunk(
                        serverLevel,
                        chunkPos,
                        new MicrowaveOvenSyncPacket(getBlockPos(), this.food)
                );
            }

            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);

            level.gameEvent(GameEvent.BLOCK_CHANGE, getBlockPos(), GameEvent.Context.of(getBlockState()));
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        this.loadAdditional(tag, provider);
        Level level = this.getLevel();
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
    }
}