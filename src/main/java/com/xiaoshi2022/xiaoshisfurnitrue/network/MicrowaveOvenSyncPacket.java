package com.xiaoshi2022.xiaoshisfurnitrue.network;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record MicrowaveOvenSyncPacket(BlockPos pos, ItemStack stack) implements CustomPacketPayload {

    public static final Type<MicrowaveOvenSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XiaoshisFurnitrue.MODID, "microwave_oven_sync"));

    public static final StreamCodec<FriendlyByteBuf, MicrowaveOvenSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public MicrowaveOvenSyncPacket decode(FriendlyByteBuf buf) {
                    BlockPos pos = buf.readBlockPos();

                    boolean hasItem = buf.readBoolean();
                    ItemStack stack;

                    if (hasItem) {
                        String itemId = buf.readUtf();
                        int count = buf.readInt();

                        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(null);
                        if (item != null) {
                            stack = new ItemStack(item, count);
                        } else {
                            stack = ItemStack.EMPTY;
                        }
                    } else {
                        stack = ItemStack.EMPTY;
                    }

                    return new MicrowaveOvenSyncPacket(pos, stack);
                }

                @Override
                public void encode(FriendlyByteBuf buf, MicrowaveOvenSyncPacket packet) {
                    buf.writeBlockPos(packet.pos());

                    ItemStack stack = packet.stack();
                    boolean hasItem = !stack.isEmpty() && stack.getItem() != null;
                    buf.writeBoolean(hasItem);

                    if (hasItem) {
                        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        buf.writeUtf(itemId.toString());
                        buf.writeInt(stack.getCount());
                    }
                }
            };

    @Override
    public @NotNull Type<MicrowaveOvenSyncPacket> type() {
        return TYPE;
    }
}
