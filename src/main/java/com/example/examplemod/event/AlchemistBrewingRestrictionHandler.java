package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class AlchemistBrewingRestrictionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.BREWING_STAND)) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.ENCHANTER_ALCHEMIST)) {
            StaminaHandler.applyProfessionalExhaustion(player, 0.05f);
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        player.displayClientMessage(
                Component.literal("§cТолько Зачарователь может варить зелья!"),
                true
        );
    }
}