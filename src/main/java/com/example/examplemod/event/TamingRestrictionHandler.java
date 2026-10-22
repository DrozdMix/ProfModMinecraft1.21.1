package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class TamingRestrictionHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Интересуют только приручаемые животные
        if (!(event.getTarget() instanceof TamableAnimal tamable)) return;

        // Уже приручённое — не трогаем
        if (tamable.isTame()) return;

        // === Исключения: кошки и попугаи — доступны всем ===
        if (tamable instanceof Cat || tamable instanceof Parrot) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.WARRIOR)) return;

        // Не Воин — отменяем взаимодействие
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);

        player.displayClientMessage(
                Component.literal("§cТолько Воин может приручать животных!"),
                true
        );
    }
}