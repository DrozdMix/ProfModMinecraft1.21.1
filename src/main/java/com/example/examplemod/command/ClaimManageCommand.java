package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.claim.ClaimManager;
import com.example.examplemod.claim.ClaimRegion;
import com.example.examplemod.network.ClaimProjectionPacket;
import com.example.examplemod.network.RegionBox;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ClaimManageCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("claim")
                        // /claim add builder|visitor <ник>
                        .then(Commands.literal("add")
                                .then(Commands.literal("builder")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> addMember(ctx, true))))
                                .then(Commands.literal("visitor")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> addMember(ctx, false))))
                        )
                        // /claim remove <ник>
                        .then(Commands.literal("remove")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            ServerLevel level = player.serverLevel();

                                            ClaimManager manager = ClaimManager.get(level);
                                            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

                                            if (region == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cТы не стоишь в регионе."));
                                                return 0;
                                            }

                                            if (!region.isOwner(player.getUUID())) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cТолько владелец может убирать права."));
                                                return 0;
                                            }

                                            region.getBuilders().remove(target.getUUID());
                                            region.getVisitors().remove(target.getUUID());
                                            manager.setDirty();

                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aПрава " + target.getName().getString() + " убраны."), false);
                                            return 1;
                                        })
                                )
                        )
                        // /claim transfer <ник>
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            ServerLevel level = player.serverLevel();

                                            ClaimManager manager = ClaimManager.get(level);
                                            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

                                            if (region == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cТы не стоишь в регионе."));
                                                return 0;
                                            }

                                            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                            boolean isMayor = data.hasProfession(Profession.MAYOR);

                                            if (!region.isOwner(player.getUUID()) && !isMayor) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cТолько владелец или Мэр может передать регион."));
                                                return 0;
                                            }

                                            region.setOwner(target.getUUID(), target.getName().getString());
                                            manager.setDirty();

                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aРегион передан " + target.getName().getString()), false);
                                            return 1;
                                        })
                                )
                        )
                        // /claim info
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    ClaimManager manager = ClaimManager.get(level);
                                    ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

                                    if (region == null) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("§eТы не в регионе."), false);
                                        return 1;
                                    }

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§6=== " + region.getName() + " ==="), false);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§7Владелец: §f" + region.getOwnerName()), false);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§7Создан: §f" + region.getFormattedDate()), false);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§7Строителей: §f" + region.getBuilders().size()), false);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§7Посетителей: §f" + region.getVisitors().size()), false);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§7Разрешённых блоков: §f" + region.getAllowedBlocks().size()), false);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§7Деспавн враждебных: §f"
                                                    + (region.isDespawnAgressive() ? "вкл" : "выкл")), false);
                                    return 1;
                                })
                        )
                        // /claim projection [...]
                        .then(Commands.literal("projection")
                                .then(Commands.literal("all")
                                        .executes(ctx -> toggleAll(ctx)))
                                .then(Commands.literal("current")
                                        .executes(ctx -> toggleCurrent(ctx)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ClaimProjectionState.disable(player.getUUID());
                                            PacketDistributor.sendToPlayer(player,
                                                    new ClaimProjectionPacket(List.of(), false));
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eОтображение выключено."), false);
                                            return 1;
                                        }))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ServerLevel level = player.serverLevel();
                                            ClaimManager manager = ClaimManager.get(level);

                                            ClaimRegion region = manager.getRegionByName(name);
                                            if (region == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cРегион '" + name + "' не найден."));
                                                return 0;
                                            }

                                            if (ClaimProjectionState.isEnabled(player.getUUID())) {
                                                ClaimProjectionState.disable(player.getUUID());
                                                PacketDistributor.sendToPlayer(player,
                                                        new ClaimProjectionPacket(List.of(), false));
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("§eОтображение выключено."), false);
                                            } else {
                                                ClaimProjectionState.enable(player.getUUID());
                                                PacketDistributor.sendToPlayer(player,
                                                        new ClaimProjectionPacket(
                                                                List.of(new RegionBox(region.getMin(), region.getMax())),
                                                                true));
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("§aОтображение региона '" + name + "' включено."), false);
                                            }
                                            return 1;
                                        })
                                )
                                .executes(ctx -> toggleCurrent(ctx))
                        )
                        // /claim delete <имя> [confirm]
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ServerLevel level = player.serverLevel();
                                            ClaimManager manager = ClaimManager.get(level);

                                            ClaimRegion region = manager.getRegionByName(name);
                                            if (region == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cРегион '" + name + "' не найден."));
                                                return 0;
                                            }

                                            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                            boolean isMayor = data.hasProfession(Profession.MAYOR);
                                            boolean isOwner = region.isOwner(player.getUUID());

                                            if (!isOwner && !isMayor) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cТолько владелец или Мэр может удалить регион."));
                                                return 0;
                                            }

                                            ClaimDeleteConfirm.setPending(player.getUUID(), name);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eВы уверены, что хотите удалить регион '" + name + "'?"), false);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eВведите §f/claim delete " + name + " confirm §eдля подтверждения."), false);
                                            return 1;
                                        })
                                        .then(Commands.literal("confirm")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    ServerLevel level = player.serverLevel();
                                                    ClaimManager manager = ClaimManager.get(level);

                                                    if (!ClaimDeleteConfirm.isPending(player.getUUID(), name)) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cСначала введите /claim delete " + name));
                                                        return 0;
                                                    }

                                                    ClaimRegion region = manager.getRegionByName(name);
                                                    if (region == null) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cРегион '" + name + "' не найден."));
                                                        ClaimDeleteConfirm.clear(player.getUUID());
                                                        return 0;
                                                    }

                                                    PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
                                                    boolean isMayor = data.hasProfession(Profession.MAYOR);
                                                    boolean isOwner = region.isOwner(player.getUUID());

                                                    if (!isOwner && !isMayor) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cТы больше не владелец этого региона — удаление отменено."));
                                                        ClaimDeleteConfirm.clear(player.getUUID());
                                                        return 0;
                                                    }

                                                    manager.removeRegion(region.getId());
                                                    manager.setDirty();
                                                    ClaimDeleteConfirm.clear(player.getUUID());

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("§aРегион '" + name + "' удалён."), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        // /claim allow — блок, на который смотрит игрок
                        .then(Commands.literal("allow")
                                .executes(ClaimManageCommand::allowLookedAtBlock)
                        )
                        // /claim disallow — блок, на который смотрит игрок
                        .then(Commands.literal("disallow")
                                .executes(ClaimManageCommand::disallowLookedAtBlock)
                        )
                        // /claim allowlist
                        .then(Commands.literal("allowlist")
                                .executes(ClaimManageCommand::listAllowed)
                        )
                        // /claim clearallowed
                        .then(Commands.literal("clearallowed")
                                .executes(ClaimManageCommand::clearAllowed)
                        )
                        // /claim despawnagressive [on|off]
                        .then(Commands.literal("despawnagressive")
                                .then(Commands.literal("on")
                                        .executes(ctx -> setDespawnAgressive(ctx, true)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> setDespawnAgressive(ctx, false)))
                                .executes(ctx -> toggleDespawnAgressive(ctx))
                        )
        );
    }

    private static int toggleCurrent(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

            if (region == null) {
                ctx.getSource().sendFailure(Component.literal("§cТы не стоишь в регионе."));
                return 0;
            }

            if (ClaimProjectionState.isEnabled(player.getUUID())) {
                ClaimProjectionState.disable(player.getUUID());
                PacketDistributor.sendToPlayer(player,
                        new ClaimProjectionPacket(List.of(), false));
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§eОтображение выключено."), false);
            } else {
                ClaimProjectionState.enable(player.getUUID());
                PacketDistributor.sendToPlayer(player,
                        new ClaimProjectionPacket(
                                List.of(new RegionBox(region.getMin(), region.getMax())),
                                true));
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§aОтображение текущего региона включено."), false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int toggleAll(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ClaimManager manager = ClaimManager.get(level);

            if (ClaimProjectionState.isEnabled(player.getUUID())) {
                ClaimProjectionState.disable(player.getUUID());
                PacketDistributor.sendToPlayer(player,
                        new ClaimProjectionPacket(List.of(), false));
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§eОтображение выключено."), false);
            } else {
                ClaimProjectionState.enable(player.getUUID());

                List<RegionBox> boxes = new ArrayList<>();
                for (ClaimRegion r : manager.getAllRegions()) {
                    boxes.add(new RegionBox(r.getMin(), r.getMax()));
                }

                PacketDistributor.sendToPlayer(player,
                        new ClaimProjectionPacket(boxes, true));
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§aОтображение всех регионов (" + boxes.size() + ") включено."), false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int addMember(CommandContext<CommandSourceStack> ctx, boolean isBuilder) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            ServerLevel level = player.serverLevel();

            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

            if (region == null) {
                ctx.getSource().sendFailure(Component.literal("§cТы не стоишь в регионе."));
                return 0;
            }

            if (!region.isOwner(player.getUUID())) {
                ctx.getSource().sendFailure(Component.literal("§cТолько владелец может добавлять права."));
                return 0;
            }

            if (isBuilder) {
                region.getBuilders().add(target.getUUID());
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§a" + target.getName().getString() + " теперь строитель."), false);
            } else {
                region.getVisitors().add(target.getUUID());
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§a" + target.getName().getString() + " теперь посетитель."), false);
            }
            manager.setDirty();
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ============ РАЗРЕШЕНИЕ БЛОКОВ ============

    private static int allowLookedAtBlock(CommandContext<CommandSourceStack> ctx) {
        return modifyLookedAtBlock(ctx, true);
    }

    private static int disallowLookedAtBlock(CommandContext<CommandSourceStack> ctx) {
        return modifyLookedAtBlock(ctx, false);
    }

    private static int modifyLookedAtBlock(CommandContext<CommandSourceStack> ctx, boolean allow) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

            if (region == null) {
                ctx.getSource().sendFailure(Component.literal("§cТы не стоишь в регионе."));
                return 0;
            }

            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
            boolean isMayor = data.hasProfession(Profession.MAYOR);
            if (!region.isOwner(player.getUUID()) && !isMayor) {
                ctx.getSource().sendFailure(
                        Component.literal("§cТолько владелец или Мэр может менять список блоков."));
                return 0;
            }

            BlockPos targetPos = getLookedAtBlock(player);
            if (targetPos == null) {
                ctx.getSource().sendFailure(
                        Component.literal("§cНаведись на блок (не дальше 10 блоков) и повтори команду."));
                return 0;
            }

            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if (block == Blocks.AIR) {
                ctx.getSource().sendFailure(Component.literal("§cНаведись на блок."));
                return 0;
            }

            var key = BuiltInRegistries.BLOCK.getKey(block);

            if (allow) {
                region.allowBlock(block);
                manager.setDirty();
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§aРазрешён блок: §f" + key), false);
            } else {
                if (!region.isBlockAllowed(block)) {
                    ctx.getSource().sendFailure(
                            Component.literal("§cБлок §f" + key + " §cне был разрешён."));
                    return 0;
                }
                region.disallowBlock(block);
                manager.setDirty();
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§eУбран блок: §f" + key), false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static BlockPos getLookedAtBlock(ServerPlayer player) {
        double reach = 10.0;
        HitResult hit = player.pick(reach, 0f, false);
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return bhr.getBlockPos();
        }
        return null;
    }

    private static int listAllowed(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

            if (region == null) {
                ctx.getSource().sendFailure(Component.literal("§cТы не стоишь в регионе."));
                return 0;
            }

            var allowed = region.getAllowedBlocks();
            if (allowed.isEmpty()) {
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§eСписок разрешённых блоков пуст."), false);
                return 1;
            }

            ctx.getSource().sendSuccess(() ->
                    Component.literal("§6=== Разрешённые блоки (" + allowed.size() + ") ==="), false);
            for (Block block : allowed) {
                var key = BuiltInRegistries.BLOCK.getKey(block);
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§a- §f" + key), false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int clearAllowed(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

            if (region == null) {
                ctx.getSource().sendFailure(Component.literal("§cТы не стоишь в регионе."));
                return 0;
            }

            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
            boolean isMayor = data.hasProfession(Profession.MAYOR);
            if (!region.isOwner(player.getUUID()) && !isMayor) {
                ctx.getSource().sendFailure(
                        Component.literal("§cТолько владелец или Мэр может это делать."));
                return 0;
            }

            int count = region.getAllowedBlocks().size();
            region.getAllowedBlocks().clear();
            manager.setDirty();

            int finalCount = count;
            ctx.getSource().sendSuccess(() ->
                    Component.literal("§eОчищено блоков: " + finalCount), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ============ ДЕСПАВН ВРАЖДЕБНЫХ ============

    private static int setDespawnAgressive(CommandContext<CommandSourceStack> ctx, boolean value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

            if (region == null) {
                ctx.getSource().sendFailure(Component.literal("§cТы не стоишь в регионе."));
                return 0;
            }

            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
            boolean isMayor = data.hasProfession(Profession.MAYOR);
            if (!region.isOwner(player.getUUID()) && !isMayor) {
                ctx.getSource().sendFailure(
                        Component.literal("§cТолько владелец или Мэр может это делать."));
                return 0;
            }

            region.setDespawnAgressive(value);
            manager.setDirty();

            ctx.getSource().sendSuccess(() ->
                    Component.literal(value
                            ? "§aДеспавн враждебных мобов включён."
                            : "§eДеспавн враждебных мобов выключен."), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int toggleDespawnAgressive(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, player.blockPosition());

            if (region == null) {
                ctx.getSource().sendFailure(Component.literal("§cТы не стоишь в регионе."));
                return 0;
            }

            boolean newValue = !region.isDespawnAgressive();
            return setDespawnAgressive(ctx, newValue);
        } catch (Exception e) {
            return 0;
        }
    }
}