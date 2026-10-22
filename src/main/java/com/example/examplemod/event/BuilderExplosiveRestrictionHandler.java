package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class BuilderExplosiveRestrictionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        var stack = event.getItemStack();
        if (!stack.is(Items.TNT) && !stack.is(Items.TNT_MINECART) && !stack.is(Items.END_CRYSTAL)) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.BUILDER)) return;

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        player.displayClientMessage(
                Component.literal("§cТолько Строитель-инженер может использовать взрывчатку!"),
                true
        );
    }
}