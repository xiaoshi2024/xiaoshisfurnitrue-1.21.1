package com.xiaoshi2022.xiaoshisfurnitrue.block.entity;

import com.xiaoshi2022.xiaoshisfurnitrue.block.RangeHoodBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RangeHoodBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("open");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RangeHoodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RANGE_HOOD_BLOCK_ENTITY.get(), pos, state);
    }

    public static final int SMOKE_ABSORB_RADIUS_H = 2;
    public static final int SMOKE_ABSORB_DEPTH = 4;

    // 每2秒吸收一次（20 tick = 1秒，40 tick = 2秒）
    private static final int ABSORB_INTERVAL = 40;

    public static void tick(Level level, BlockPos pos, BlockState state, RangeHoodBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        if (!state.getValue(RangeHoodBlock.POWERED)) {
            return;
        }
        // 每2秒执行一次吸收
        if ((level.getGameTime() % ABSORB_INTERVAL) != 0) {
            return;
        }

        RangeHoodBlock.absorbSmoke(level, pos);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T entity) {
        if (entity instanceof RangeHoodBlockEntity hood) {
            tick(level, pos, state, hood);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<RangeHoodBlockEntity> leafController = new AnimationController<>(
                this, "leaf_controller", 0, state -> {
            BlockState blockState = getBlockState();
            if (blockState.getValue(RangeHoodBlock.OPEN)) {
                return state.setAndContinue(OPEN);
            }
            return state.setAndContinue(IDLE);
        });

        controllers.add(leafController);
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

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        this.loadAdditional(tag, provider);
    }
}