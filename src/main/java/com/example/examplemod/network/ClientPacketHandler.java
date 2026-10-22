package com.example.examplemod.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static List<RegionBox> activeRegions = new ArrayList<>();

    public static void handleProjection(ClaimProjectionPacket packet,
                                        net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.enable()) {
                activeRegions = new ArrayList<>(packet.regions());
            } else {
                activeRegions = new ArrayList<>();
            }
        });
    }
}