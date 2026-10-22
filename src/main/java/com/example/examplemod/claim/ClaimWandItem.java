package com.example.examplemod.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimWandItem extends Item {

    private static final Map<UUID, BlockPos> FIRST_CORNER = new HashMap<>();
    private static final Map<UUID, BlockPos> SECOND_CORNER = new HashMap<>();

    public ClaimWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        UUID uuid = player.getUUID();

        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!FIRST_CORNER.containsKey(uuid)) {
            FIRST_CORNER.put(uuid, pos);
            player.displayClientMessage(
                    Component.literal("§aУгол 1: " + pos.toShortString()), false);
        } else if (!SECOND_CORNER.containsKey(uuid)) {
            SECOND_CORNER.put(uuid, pos);
            player.displayClientMessage(
                    Component.literal("§aУгол 2: " + pos.toShortString()), false);
            player.displayClientMessage(
                    Component.literal("§eТеперь используй /claim create <название>"), false);
        } else {
            FIRST_CORNER.put(uuid, pos);
            SECOND_CORNER.remove(uuid);
            player.displayClientMessage(
                    Component.literal("§eНовое выделение. Угол 1: " + pos.toShortString()), false);
        }

        return InteractionResult.SUCCESS;
    }

    public static BlockPos getFirstCorner(UUID uuid) { return FIRST_CORNER.get(uuid); }
    public static BlockPos getSecondCorner(UUID uuid) { return SECOND_CORNER.get(uuid); }
    public static void clear(UUID uuid) {
        FIRST_CORNER.remove(uuid);
        SECOND_CORNER.remove(uuid);
    }
}