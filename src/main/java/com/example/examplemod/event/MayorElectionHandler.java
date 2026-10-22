package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.mayor.MayorElectionData;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import com.example.examplemod.profession.ProfessionSync;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class MayorElectionHandler {

    private static int reminderTickCounter = 0;
    private static int dueCheckTickCounter = 0;

    /**
     * При входе игрока:
     *  1. Снимаем "висячее" звание Мэра, если игрок больше не текущий Мэр.
     *  2. Выдаём звание pending-Мэру (победившему оффлайн).
     *  3. Синхронизируем профессию — чтобы значок в Tab появился сразу.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        MayorElectionData election = MayorElectionData.get(level);
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);

        // 1. Снимаем "висячее" звание
        if (data.getOptional() == Profession.MAYOR
                && !player.getUUID().equals(election.getCurrentMayor())) {
            data.setOptional(Profession.NONE);
        }

        // 2. Выдаём звание pending-Мэру
        if (player.getUUID().equals(election.getPendingMayor())) {
            data.setOptional(Profession.MAYOR);
            election.clearPendingMayor();
            player.displayClientMessage(
                    Component.literal("§6Вы были избраны Мэром (пока были оффлайн)."), false);
        }

        // 3. Синхронизируем — обновляет Tab и клиентские данные
        ProfessionSync.sync(player);
    }

    /**
     * При респавне после смерти переносим профессии со старого игрока на нового.
     * Подстраховка на случай, если copyOnDeath() не сработает.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        PlayerProfessionData oldData = event.getOriginal().getData(ExampleMod.PROFESSION_DATA);
        PlayerProfessionData newData = event.getEntity().getData(ExampleMod.PROFESSION_DATA);

        newData.setPrimary(oldData.getPrimary());
        newData.setOptional(oldData.getOptional());

        if (event.getEntity() instanceof ServerPlayer sp) {
            ProfessionSync.sync(sp);
        }
    }

    /** Автозапуск плановых выборов + напоминания + отмена recall. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        dueCheckTickCounter++;
        if (dueCheckTickCounter >= 200) { // раз в 10 секунд
            dueCheckTickCounter = 0;

            ServerLevel overworld = event.getServer().overworld();
            MayorElectionData election = MayorElectionData.get(overworld);

            // Плановые выборы по истечении недели
            if (!election.isElectionActive() && election.isElectionDue() && election.getCurrentMayor() != null) {
                election.startElection(false);

                overworld.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§6Срок Мэра истёк! Начались выборы. Используйте /mayor vote <ник>"), false);
            }

            // Отмена recall, если не набрали 2 голоса за минуту
            if (election.isElectionActive()
                    && election.isRecall()
                    && election.getVoteCount() < MayorElectionData.RECALL_MIN_VOTES
                    && election.isRecallWindowExpired()) {
                election.cancelElection();
                overworld.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§eПеревыборы отменены: не набрано 2 голоса за минуту."), false);
            }
        }

        reminderTickCounter++;
        if (reminderTickCounter < 6000) return; // 6000 тиков = 5 минут
        reminderTickCounter = 0;

        ServerLevel overworld = event.getServer().overworld();
        MayorElectionData election = MayorElectionData.get(overworld);

        if (election.isElectionActive()) {
            overworld.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§e[Выборы] Голосование продолжается! /mayor vote <ник>"), false);
        }
    }
}