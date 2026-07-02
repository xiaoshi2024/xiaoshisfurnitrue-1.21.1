package com.xiaoshi2022.xiaoshisfurnitrue.network;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.MicrowaveOvenBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MicrowaveOvenSyncHandlerClient {
    @OnlyIn(Dist.CLIENT)
    public static void handle(final MicrowaveOvenSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            XiaoshisFurnitrue.LOGGER.warn("Level is null, cannot sync microwave oven");
            return;
        }

        BlockPos pos = packet.pos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MicrowaveOvenBlockEntity oven) {
            ItemStack stack = packet.stack();
            XiaoshisFurnitrue.LOGGER.debug("Client received microwave oven sync: pos={}, hasStack={}",
                    pos, !stack.isEmpty());

            oven.setFoodClient(stack);

            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        } else {
            XiaoshisFurnitrue.LOGGER.warn("BlockEntity at {} is not MicrowaveOvenBlockEntity", pos);
        }
    }
}
