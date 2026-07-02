package com.xiaoshi2022.xiaoshisfurnitrue.network;

import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    public static void registerPackets(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::registerNetworkPackets);
    }

    public static void registerNetworkPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(XiaoshisFurnitrue.MODID).versioned("1.0.0");

        registrar.playToClient(
                MicrowaveOvenSyncPacket.TYPE,
                MicrowaveOvenSyncPacket.STREAM_CODEC,
                MicrowaveOvenSyncHandler::handleClient
        );
    }
}
