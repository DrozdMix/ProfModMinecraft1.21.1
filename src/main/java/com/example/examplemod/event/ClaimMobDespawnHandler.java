package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.claim.ClaimManager;
import com.example.examplemod.claim.ClaimRegion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Деспавн враждебных мобов на территории привата,
 * если у привата включён флаг despawnAgressive.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public class ClaimMobDespawnHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        tickCounter++;
        if (tickCounter < 60) return; // раз в 3 секунды
        tickCounter = 0;

        ClaimManager manager = ClaimManager.get(level);

        // Проходим по всем сущностям уровня
        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;
            if (!(mob instanceof Enemy)) continue; // только враждебные

            ClaimRegion region = manager.getRegionAt(level, mob.blockPosition());
            if (region == null) continue;
            if (!region.isDespawnAgressive()) continue;

            // Удаляем моба
            mob.discard();
        }
    }
}