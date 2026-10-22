package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.compat.FarmersDelightCompat;
import com.example.examplemod.compat.ModCompat;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class FarmersDelightRestrictionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ModCompat.isFarmersDelightLoaded()) return;

        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (!FarmersDelightCompat.isFDCookingBlock(event.getLevel().getBlockState(event.getPos()))) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.FARMER_COOK)) return;

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        player.displayClientMessage(
                Component.literal("§cТолько Фермер-повар может использовать кухонные блоки!"),
                true
        );
    }
}