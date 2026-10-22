package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.claim.ClaimManager;
import com.example.examplemod.claim.ClaimRegion;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ClaimProtectionHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        ClaimManager manager = ClaimManager.get(level);
        ClaimRegion region = manager.getRegionAt(level, event.getPos());

        if (region == null) return;

        if (region.canBuild(player.getUUID())) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.MAYOR)) return;

        event.setCanceled(true);
        player.displayClientMessage(
                Component.literal("§cЭта территория защищена!"), true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        ClaimManager manager = ClaimManager.get(level);
        ClaimRegion region = manager.getRegionAt(level, event.getPos());

        if (region == null) return;

        if (region.canBuild(player.getUUID())) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.MAYOR)) return;

        event.setCanceled(true);
        player.displayClientMessage(
                Component.literal("§cЭта территория защищена!"), true);
    }
}