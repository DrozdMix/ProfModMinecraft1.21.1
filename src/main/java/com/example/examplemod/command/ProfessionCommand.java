package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import com.example.examplemod.profession.ProfessionSync;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ProfessionCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("profession")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("role", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (Profession p : Profession.values()) {
                                                if (p != Profession.NONE && !p.isOptional()) {
                                                    builder.suggest(p.getId());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String roleId = StringArgumentType.getString(ctx, "role");
                                            Profession profession = Profession.byId(roleId);

                                            if (profession == Profession.NONE || profession.isOptional()) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cНельзя поставить в основную роль: " + roleId));
                                                return 0;
                                            }

                                            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                            data.setPrimary(profession);
                                            ProfessionSync.sync(player);

                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("Роль установлена: " + profession.getId()), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("setoptional")
                                .then(Commands.argument("role", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (Profession p : Profession.values()) {
                                                if (p.isOptional() && p != Profession.MAYOR) {
                                                    builder.suggest(p.getId());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String roleId = StringArgumentType.getString(ctx, "role");
                                            Profession profession = Profession.byId(roleId);

                                            if (profession == Profession.MAYOR) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cРоль Мэра выдаётся только через выборы."));
                                                return 0;
                                            }

                                            if (!profession.isOptional()) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cЭто не опциональная роль: " + roleId));
                                                return 0;
                                            }

                                            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                            if (data.getPrimary() == profession) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cЭта роль уже стоит как основная."));
                                                return 0;
                                            }

                                            data.setOptional(profession);
                                            ProfessionSync.sync(player);

                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("Опциональная роль: " + profession.getId()), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("clearoptional")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                    data.setOptional(Profession.NONE);
                                    ProfessionSync.sync(player);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Опциональная роль снята."), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                    data.setPrimary(Profession.NONE);
                                    data.setOptional(Profession.NONE);
                                    ProfessionSync.sync(player);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§eВсе профессии сняты."), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("get")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Основная: " + data.getPrimary().getId()
                                                    + " | Опциональная: " + data.getOptional().getId()), false);
                                    return 1;
                                })
                        )
        );
    }
}