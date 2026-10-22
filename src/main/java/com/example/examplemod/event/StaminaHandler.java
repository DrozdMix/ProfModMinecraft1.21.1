package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Стамина: добыча ресурсов и профессиональная деятельность понемногу
 * снижают сытость игрока (через vanilla-exhaustion — та же величина, что
 * тратится на бег/прыжки, естественным образом уменьшает уровень голода).
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public class StaminaHandler {

    /** +0.05 голода за каждый сломанный блок (добыча ресурсов: руда, дерево и т.п.). */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        applyProfessionalExhaustion(player, 0.05f);
    }

    /** Тратит "стамину" (голод) за успешное профессиональное действие. */
    public static void applyProfessionalExhaustion(Player player, float amount) {
        if (player.level().isClientSide()) return;
        player.getFoodData().addExhaustion(amount);
    }
}
