package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.mayor.MayorElectionData;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import com.example.examplemod.profession.ProfessionSync;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class MayorCommand {

    private static final int GREEN = 0x00FF00;
    private static final int RED = 0xFF0000;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("mayor")
                        // /mayor setrole <игрок> <роль|none>
                        .then(Commands.literal("setrole")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("none");
                                                    for (Profession p : Profession.values()) {
                                                        if (p != Profession.NONE && p != Profession.MAYOR) {
                                                            builder.suggest(p.getId());
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    ServerPlayer mayor = ctx.getSource().getPlayerOrException();

                                                    PlayerProfessionData mayorData = mayor.getData(ExampleMod.PROFESSION_DATA);
                                                    if (!mayorData.hasProfession(Profession.MAYOR)) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cТолько Мэр может выдавать роли!"));
                                                        return 0;
                                                    }

                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    String roleId = StringArgumentType.getString(ctx, "role");
                                                    PlayerProfessionData targetData = target.getData(ExampleMod.PROFESSION_DATA);

                                                    if (roleId.equalsIgnoreCase("none")) {
                                                        targetData.setPrimary(Profession.NONE);
                                                        targetData.setOptional(Profession.NONE);
                                                        ProfessionSync.sync(target);

                                                        ctx.getSource().sendSuccess(() ->
                                                                Component.literal("§a" + target.getName().getString() + " лишён профессии."), true);
                                                        target.displayClientMessage(
                                                                Component.literal("§eС вас сняли профессию."), false);
                                                        return 1;
                                                    }

                                                    Profession profession = Profession.byId(roleId);

                                                    if (profession == Profession.NONE) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cНеизвестная роль: " + roleId));
                                                        return 0;
                                                    }

                                                    if (profession == Profession.MAYOR) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cРоль Мэра нельзя выдать вручную — только через выборы (/mayor vote)."));
                                                        return 0;
                                                    }

                                                    if (profession.isOptional()) {
                                                        targetData.setOptional(profession);
                                                    } else {
                                                        targetData.setPrimary(profession);
                                                    }
                                                    ProfessionSync.sync(target);

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("§a" + target.getName().getString()
                                                                    + " получил роль: " + profession.getId()), true);
                                                    target.displayClientMessage(
                                                            Component.literal("§eВам выдана роль: " + profession.getId()), false);

                                                    return 1;
                                                })
                                        )
                                )
                        )
                        // /mayor whoami
                        .then(Commands.literal("whoami")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                    boolean isMayor = data.hasProfession(Profession.MAYOR);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal(isMayor
                                                    ? "§aТы — Мэр."
                                                    : "§cТы не Мэр."), false);
                                    return 1;
                                })
                        )
                        // /mayor vote <кандидат>
                        .then(Commands.literal("vote")
                                .then(Commands.argument("candidate", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer voter = ctx.getSource().getPlayerOrException();
                                            ServerPlayer candidate = EntityArgument.getPlayer(ctx, "candidate");

                                            ServerLevel level = voter.serverLevel();
                                            MayorElectionData election = MayorElectionData.get(level);

                                            if (!election.isElectionActive()) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cГолосование сейчас не идёт. Начать переизбрание: /mayor recall"));
                                                return 0;
                                            }

                                            election.vote(voter.getUUID(), candidate.getUUID());
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aГолос за " + candidate.getName().getString() + " учтён."), false);

                                            candidate.displayClientMessage(
                                                    Component.literal("§e" + voter.getName().getString() + " проголосовал за вас."), false);
                                            return 1;
                                        })
                                )
                        )
                        // /mayor recall — досрочно инициировать перевыборы (нужно 2 голоса за минуту)
                        .then(Commands.literal("recall")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    MayorElectionData election = MayorElectionData.get(level);

                                    if (election.isElectionActive()) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cГолосование уже идёт! /mayor vote <ник>"));
                                        return 0;
                                    }

                                    election.startElection(true);
                                    election.touchRecallWindow();

                                    level.getServer().getPlayerList().broadcastSystemMessage(
                                            Component.literal("§6" + player.getName().getString()
                                                    + " инициировал(а) досрочные перевыборы Мэра! Нужно минимум 2 голоса за минуту. Голосуйте: /mayor vote <ник>"), false);
                                    return 1;
                                })
                        )
                        // /mayor finish
                        .then(Commands.literal("finish")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    MayorElectionData election = MayorElectionData.get(level);

                                    if (!election.isElectionActive()) {
                                        ctx.getSource().sendFailure(Component.literal("§cГолосование сейчас не идёт."));
                                        return 0;
                                    }

                                    boolean isAdmin = ctx.getSource().hasPermission(2);
                                    boolean dayPassed = election.isDayPassedSinceStart();
                                    if (!isAdmin && !dayPassed) {
                                        long leftMs = MayorElectionData.DAY_MS
                                                - (System.currentTimeMillis() - election.getElectionStartedAt());
                                        long leftHours = Math.max(0, leftMs / (60L * 60L * 1000L));
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cЗавершить голосование раньше времени может только администратор. "
                                                        + "Либо подождите ещё ~" + leftHours + " ч."));
                                        return 0;
                                    }

                                    UUID winner = election.getWinner();
                                    if (winner == null) {
                                        ctx.getSource().sendFailure(Component.literal("§cНет голосов."));
                                        return 0;
                                    }

                                    // Снимаем звание с прежнего Мэра
                                    UUID previousMayor = election.getCurrentMayor();
                                    if (previousMayor != null && !previousMayor.equals(winner)) {
                                        ServerPlayer previousPlayer = level.getServer().getPlayerList().getPlayer(previousMayor);
                                        if (previousPlayer != null) {
                                            PlayerProfessionData prevData = previousPlayer.getData(ExampleMod.PROFESSION_DATA);
                                            if (prevData.getOptional() == Profession.MAYOR) {
                                                prevData.setOptional(Profession.NONE);
                                                ProfessionSync.sync(previousPlayer);
                                            }
                                        }
                                    }

                                    ServerPlayer winnerPlayer = level.getServer().getPlayerList().getPlayer(winner);
                                    if (winnerPlayer == null) {
                                        // Оффлайн — сохраняем как pending, выдадим при входе
                                        election.setPendingMayor(winner);
                                        election.setMayor(winner);

                                        String name = level.getServer().getProfileCache()
                                                .get(winner)
                                                .map(GameProfile::getName)
                                                .orElse(winner.toString());

                                        level.getServer().getPlayerList().broadcastSystemMessage(
                                                Component.literal("§6Новый Мэр: " + name + " (оффлайн, получит роль при входе)"), false);
                                        return 1;
                                    }

                                    PlayerProfessionData data = winnerPlayer.getData(ExampleMod.PROFESSION_DATA);
                                    data.setOptional(Profession.MAYOR);
                                    ProfessionSync.sync(winnerPlayer);

                                    election.setMayor(winner);

                                    level.getServer().getPlayerList().broadcastSystemMessage(
                                            Component.literal("§6Новый Мэр: " + winnerPlayer.getName().getString()), false);

                                    return 1;
                                })
                        )
                        // /mayor election
                        .then(Commands.literal("election")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    MayorElectionData election = MayorElectionData.get(level);

                                    // Имя текущего Мэра
                                    UUID mayorId = election.getCurrentMayor();
                                    String mayorName = "§7нет";
                                    if (mayorId != null) {
                                        ServerPlayer mayor = level.getServer().getPlayerList().getPlayer(mayorId);
                                        if (mayor != null) {
                                            mayorName = "§f" + mayor.getName().getString();
                                        } else {
                                            mayorName = "§7" + level.getServer().getProfileCache()
                                                    .get(mayorId)
                                                    .map(GameProfile::getName)
                                                    .orElse(mayorId.toString());
                                        }
                                    }

                                    if (election.isElectionActive()) {
                                        String finalMayorName = mayorName;
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal((election.isRecall() ? "§e[Перевыборы] " : "§e[Плановые выборы] ")
                                                        + "Голосование идёт! Текущий мэр: " + finalMayorName), false);

                                        Map<UUID, Integer> counts = new HashMap<>();
                                        for (UUID c : election.getVotes().values()) {
                                            counts.merge(c, 1, Integer::sum);
                                        }

                                        if (counts.isEmpty()) {
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§7Голосов пока нет."), false);
                                        } else {
                                            int total = election.getVoteCount();
                                            for (Map.Entry<UUID, Integer> e : counts.entrySet()) {
                                                ServerPlayer candidate = level.getServer().getPlayerList().getPlayer(e.getKey());
                                                String candName = candidate != null
                                                        ? candidate.getName().getString()
                                                        : e.getKey().toString();
                                                int votes = e.getValue();

                                                ctx.getSource().sendSuccess(() ->
                                                                Component.literal("§f" + candName + ": ")
                                                                        .append(Component.literal(String.valueOf(votes)).withStyle(Style.EMPTY.withColor(GREEN)))
                                                                        .append(Component.literal("/" + total).withStyle(Style.EMPTY.withColor(RED))),
                                                        false);
                                            }
                                        }
                                        return 1;
                                    }

                                    long ms = election.getTimeUntilElection();
                                    long days = ms / (24L * 60L * 60L * 1000L);
                                    long hours = (ms / (60L * 60L * 1000L)) % 24;

                                    String finalMayorName1 = mayorName;
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§eТекущий мэр: " + finalMayorName1), false);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§eДо плановых выборов: " + days + " д. " + hours
                                                    + " ч. Хотите переизбрать Мэра раньше? /mayor recall"), false);
                                    return 1;
                                })
                        )
        );
    }
}