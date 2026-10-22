package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.claim.ClaimManager;
import com.example.examplemod.claim.ClaimRegion;
import com.example.examplemod.claim.ClaimWandItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ClaimCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("claim")
                        // /claim create <name>
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");

                                            BlockPos corner1 = ClaimWandItem.getFirstCorner(player.getUUID());
                                            BlockPos corner2 = ClaimWandItem.getSecondCorner(player.getUUID());

                                            if (corner1 == null || corner2 == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("Сначала выдели два угла палочкой!"));
                                                return 0;
                                            }

                                            ServerLevel level = player.serverLevel();
                                            ClaimManager manager = ClaimManager.get(level);

                                            if (manager.getRegionByName(name) != null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cРегион с именем '" + name + "' уже существует."));
                                                return 0;
                                            }

                                            BlockPos newMin = new BlockPos(
                                                    Math.min(corner1.getX(), corner2.getX()),
                                                    Math.min(corner1.getY(), corner2.getY()),
                                                    Math.min(corner1.getZ(), corner2.getZ()));
                                            BlockPos newMax = new BlockPos(
                                                    Math.max(corner1.getX(), corner2.getX()),
                                                    Math.max(corner1.getY(), corner2.getY()),
                                                    Math.max(corner1.getZ(), corner2.getZ()));
                                            String dimension = level.dimension().location().toString();

                                            for (ClaimRegion existing : manager.getAllRegions()) {
                                                if (existing.intersects(newMin, newMax, dimension)) {
                                                    ctx.getSource().sendFailure(
                                                            Component.literal("§cОбласть пересекается с регионом '"
                                                                    + existing.getName() + "' (владелец: " + existing.getOwnerName() + ")."));
                                                    return 0;
                                                }
                                            }

                                            ClaimRegion region = new ClaimRegion(
                                                    name, corner1, corner2,
                                                    player.getUUID(),
                                                    player.getName().getString(),
                                                    level);
                                            manager.addRegion(region);

                                            ClaimWandItem.clear(player.getUUID());

                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aРегион '" + name + "' создан!"), false);
                                            return 1;
                                        })
                                )
                        )
                        // /claim list
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    ClaimManager manager = ClaimManager.get(level);

                                    var regions = manager.getAllRegions();
                                    if (regions.isEmpty()) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("§eНет созданных регионов."), false);
                                        return 1;
                                    }

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§6=== Регионы (" + regions.size() + ") ==="), false);

                                    for (ClaimRegion region : regions) {
                                        String info = "§a" + region.getName()
                                                + " §7| §f" + region.getMin().toShortString()
                                                + " §7-> §f" + region.getMax().toShortString()
                                                + " §7| владелец: §f" + region.getOwnerName()
                                                + " §7| создан: §f" + region.getFormattedDate();
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal(info), false);
                                    }
                                    return 1;
                                })
                        )
        );
    }
}