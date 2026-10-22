package com.example.examplemod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ClaimProjectionPacket(List<RegionBox> regions, boolean enable)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClaimProjectionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    "examplemod", "claim_projection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimProjectionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    RegionBox.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ClaimProjectionPacket::regions,
                    ByteBufCodecs.BOOL,
                    ClaimProjectionPacket::enable,
                    ClaimProjectionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}