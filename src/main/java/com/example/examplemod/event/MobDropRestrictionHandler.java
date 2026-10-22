package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class MobDropRestrictionHandler {

    /** Проверяем, что убийца имеет право на дроп. */
    private static boolean hasDropRight(Player player, boolean isAnimal) {
        if (player == null) return false;
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);

        // Воин получает дроп с мобов
        if (!isAnimal && data.hasProfession(Profession.WARRIOR)) return true;

        // Фермер получает дроп с животных
        if (isAnimal && data.hasProfession(Profession.FARMER_COOK)) return true;

        return false;
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        boolean isAnimal = mob instanceof Animal;

        Player killer = null;
        if (event.getSource().getEntity() instanceof Player player) {
            killer = player;
        }

        if (hasDropRight(killer, isAnimal)) return;

        event.getDrops().clear();
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        boolean isAnimal = mob instanceof Animal;

        Player killer = event.getAttackingPlayer();

        if (hasDropRight(killer, isAnimal)) return;

        event.setDroppedExperience(0);
    }
}