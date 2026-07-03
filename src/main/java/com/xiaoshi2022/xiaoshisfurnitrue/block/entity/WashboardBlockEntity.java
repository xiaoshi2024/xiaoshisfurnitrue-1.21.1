package com.xiaoshi2022.xiaoshisfurnitrue.block.entity;

import com.xiaoshi2022.xiaoshisfurnitrue.block.WashboardBlock;
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

public class WashboardBlockEntity extends BlockEntity implements GeoBlockEntity {

    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation LAY = RawAnimation.begin().thenPlayAndHold("lay");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WashboardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASHBOARD_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, this::animController));
    }

    protected <E extends WashboardBlockEntity> PlayState animController(final AnimationState<E> state) {
        BlockState blockstate = getBlockState();

        if (blockstate.getValue(WashboardBlock.ACTIVE)) {
            return state.setAndContinue(LAY);
        }

        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ===== 服务器端tick =====
    public void tick(Level level, BlockPos pos, BlockState state) {
        // 服务器端逻辑
    }

    // ===== 数据同步：服务器 -> 客户端 =====
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