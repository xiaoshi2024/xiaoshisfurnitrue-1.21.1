package com.xiaoshi2022.xiaoshisfurnitrue.network;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MicrowaveOvenSyncHandler {
    public static void handleClient(final MicrowaveOvenSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> handlerClass = Class.forName("com.xiaoshi2022.xiaoshisfurnitrue.network.MicrowaveOvenSyncHandlerClient");
                java.lang.reflect.Method method = handlerClass.getMethod("handle", MicrowaveOvenSyncPacket.class);
                method.invoke(null, packet);
            } catch (Exception e) {
                XiaoshisFurnitrue.LOGGER.warn("Failed to handle MicrowaveOvenSyncPacket (likely server-side)", e);
            }
        });
    }
}
