package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.event.ArmorRestrictionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class ArmorSlotClickMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void examplemod$blockArmorClick(int slotId, int button, ClickType clickType,
                                            Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= menu.slots.size()) return;

        Slot slot = menu.slots.get(slotId);
        EquipmentSlot equipSlot = player.getEquipmentSlotForItem(slot.getItem());

        if (!equipSlot.isArmor()) return;

        ItemStack cursor = menu.getCarried();
        if (cursor.isEmpty()) return;

        if (!ArmorRestrictionHandler.isHeavyArmor(cursor)) return;
        if (ArmorRestrictionHandler.canWear(player)) return;

        ci.cancel();

        // Синхронизируем слот и курсор с клиентом
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            // Отправляем текущее состояние слота брони
            sp.connection.send(new ClientboundContainerSetSlotPacket(
                    menu.containerId, menu.incrementStateId(), slotId, slot.getItem()
            ));
            // Отправляем текущее состояние курсора
            sp.connection.send(new ClientboundContainerSetSlotPacket(
                    -1, menu.incrementStateId(), -1, menu.getCarried()
            ));
        }

        player.displayClientMessage(
                Component.literal("§cЭта броня доступна только Воину и Шахтёру!"),
                true
        );
    }
}