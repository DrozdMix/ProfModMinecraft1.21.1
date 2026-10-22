package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SaplingBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class LumberjackSaplingRestrictionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Проверяем, что в руке — саженец
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof SaplingBlock)) return;

        // Проверяем роль
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.LUMBERJACK)) return;

        // Не Лесоруб — запрещаем посадку
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);

        player.displayClientMessage(
                Component.literal("§cТолько Лесоруб может сажать саженцы!"),
                true
        );
    }
}