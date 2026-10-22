package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class FishermanFishingRestrictionHandler {

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.FISHERMAN)) {
            StaminaHandler.applyProfessionalExhaustion(player, 0.1f);
            return;
        }

        // Не Рыбак — отменяем вылов
        event.setCanceled(true);
        player.displayClientMessage(
                Component.literal("§cТолько Рыбак может рыбачить!"),
                true
        );
    }
}