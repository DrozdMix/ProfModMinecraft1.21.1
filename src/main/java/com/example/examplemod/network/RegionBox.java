package com.example.examplemod.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record RegionBox(BlockPos min, BlockPos max) {
    public static final StreamCodec<RegistryFriendlyByteBuf, RegionBox> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RegionBox::min,
                    BlockPos.STREAM_CODEC, RegionBox::max,
                    RegionBox::new
            );
}