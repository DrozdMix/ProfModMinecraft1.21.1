package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ArmorEquipRestrictionHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack stack = event.getItemStack();
        if (!ArmorRestrictionHandler.isHeavyArmor(stack)) return;
        if (ArmorRestrictionHandler.canWear(player)) return;

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);

        // Форсируем синхронизацию инвентаря и хотбара на клиенте
        if (player instanceof ServerPlayer sp) {
            // Синхронизируем весь инвентарь
            sp.connection.send(new ClientboundContainerSetContentPacket(
                    sp.inventoryMenu.containerId,
                    sp.inventoryMenu.incrementStateId(),
                    sp.inventoryMenu.getItems(),
                    sp.inventoryMenu.getCarried()
            ));
            // Синхронизируем активный слот хотбара
            sp.connection.send(new ClientboundSetCarriedItemPacket(sp.getInventory().selected));
        }

        player.displayClientMessage(
                Component.literal("§cЭта броня доступна только Воину и Шахтёру!"),
                true
        );
    }
}