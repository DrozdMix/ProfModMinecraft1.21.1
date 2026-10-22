package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Сон на сервере: если хотя бы половина игроков лежит в кроватях,
 * ночь пропускается, даже если кто-то ещё не спит.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public class SleepVoteHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < 20) return; // раз в секунду
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();

        // Ночь?
        if (!overworld.isNight()) return;

        var players = overworld.players();
        if (players.isEmpty()) return;

        int total = players.size();
        int sleeping = 0;
        int needToSkip = Math.max(1, (int) Math.ceil(total / 2.0));

        for (ServerPlayer p : players) {
            if (p.isSleeping()) sleeping++;
        }

        // Половина и больше — но не все (всех обрабатывает ванилла)
        if (sleeping >= needToSkip && sleeping < total) {
            long timeToMorning = 24000L - overworld.getDayTime() % 24000L;
            overworld.setDayTime(overworld.getDayTime() + timeToMorning);

            for (ServerPlayer p : players) {
                if (p.isSleeping()) {
                    p.stopSleepInBed(true, true);
                }
            }

            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§eБольшинство игроков спит — ночь пропущена."), false);
        }
    }
}