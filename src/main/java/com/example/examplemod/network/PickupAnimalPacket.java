package com.example.examplemod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PickupAnimalPacket(UUID animalUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PickupAnimalPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    "examplemod", "pickup_animal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PickupAnimalPacket> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.core.UUIDUtil.STREAM_CODEC, PickupAnimalPacket::animalUuid,
                    PickupAnimalPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}