package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class WarriorDamageHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Нас интересуют только атаки по мобам
        if (!(event.getEntity() instanceof Mob)) return;

        // Атакующий должен быть игроком
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // Проверяем, что игрок — Воин
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (!data.hasProfession(Profession.WARRIOR)) return;

        // +20% урона
        float newDamage = event.getAmount() * 1.2f;
        event.setAmount(newDamage);

        StaminaHandler.applyProfessionalExhaustion(player, 0.02f);
    }
}