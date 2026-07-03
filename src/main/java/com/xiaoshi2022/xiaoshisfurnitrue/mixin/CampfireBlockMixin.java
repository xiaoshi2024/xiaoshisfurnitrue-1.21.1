package com.xiaoshi2022.xiaoshisfurnitrue.mixin;

import com.xiaoshi2022.xiaoshisfurnitrue.block.RangeHoodBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlock.class)
public class CampfireBlockMixin {

    private static final int MAX_RADIUS = 2;
    private static final int MAX_HEIGHT = 4;

    // 每2秒检测一次（20 tick = 1秒，40 tick = 2秒）
    private static final int CHECK_INTERVAL = 40;

    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void onAnimateTick(BlockState state, Level level, BlockPos pos,
                               RandomSource random, CallbackInfo ci) {
        if (!level.isClientSide()) return;

        // 每2秒检测一次（40 tick）
        if (random.nextInt(CHECK_INTERVAL) != 0) return;

        // 快速检查：先查正上方
        for (int y = 0; y <= MAX_HEIGHT; y++) {
            BlockPos abovePos = pos.above(y);
            if (isPoweredRangeHood(level, abovePos)) {
                ci.cancel();
                return;
            }

            // 再查周围
            for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx++) {
                for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos checkPos = abovePos.offset(dx, 0, dz);
                    if (isPoweredRangeHood(level, checkPos)) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }

    private boolean isPoweredRangeHood(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RangeHoodBlock)) {
            return false;
        }
        return state.getValue(RangeHoodBlock.POWERED);
    }
}