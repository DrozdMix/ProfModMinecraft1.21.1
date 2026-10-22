package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ExampleMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Сервер → клиент (проекция)
        registrar.playToClient(
                ClaimProjectionPacket.TYPE,
                ClaimProjectionPacket.STREAM_CODEC,
                ModNetwork::handleProjectionOnClient
        );

        // Клиент → сервер (подбор животного)
        registrar.playToServer(
                PickupAnimalPacket.TYPE,
                PickupAnimalPacket.STREAM_CODEC,
                ServerPacketHandler::handlePickupAnimal
        );
    }

    /** Обёртка, которая безопасно вызывает клиентский обработчик. */
    private static void handleProjectionOnClient(ClaimProjectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.handleProjection(packet, context));
    }
}