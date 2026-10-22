package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.claim.ClaimManager;
import com.example.examplemod.claim.ClaimRegion;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ClaimInteractionProtectionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        ClaimManager manager = ClaimManager.get(level);
        ClaimRegion region = manager.getRegionAt(level, event.getPos());

        if (region == null) return;

        // === Разрешённые блоки — можно всем ===
        BlockState state = level.getBlockState(event.getPos());
        if (region.isBlockAllowed(state.getBlock())) return;

        // Владелец, строитель или посетитель
        if (region.canVisit(player.getUUID())) return;

        // Мэр
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.MAYOR)) return;

        // Запрет
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        player.displayClientMessage(
                Component.literal("§cЭта территория защищена!"), true);
    }
}