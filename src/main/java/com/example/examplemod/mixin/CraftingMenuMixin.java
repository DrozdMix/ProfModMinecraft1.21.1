package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import com.example.examplemod.util.CraftGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin extends AbstractContainerMenu {

    protected CraftingMenuMixin() {
        super(null, 0);
    }

    /** Блокирует шифт-клик. Обычный клик по результату блокируется в {@link SlotMixin}. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void examplemod$blockBuildingCraft(Player player, int index,
                                               CallbackInfoReturnable<ItemStack> cir) {
        if (index != 0) return;
        if (player.level().isClientSide()) return;

        ItemStack stack = this.slots.get(0).getItem();
        if (stack.isEmpty()) return;

        if (!CraftGuard.isTakeAllowed(player, stack)) {
            cir.setReturnValue(ItemStack.EMPTY);
            player.displayClientMessage(
                    Component.literal("§cТолько Строитель-инженер может крафтить этот предмет!"),
                    true
            );
            return;
        }

        // Логика прочности
        if (!stack.isDamageableItem()) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);

        // === Удочки ===
        // Рыбак — 100%. Все остальные — 5%.
        if (stack.getItem() instanceof FishingRodItem) {
            if (data.hasProfession(Profession.FISHERMAN)) return;

            int maxDamage = stack.getMaxDamage();
            stack.setDamageValue((int) (maxDamage * 0.95));
            return;
        }

        // === Всё остальное ===
        // Кузнец — 100%. Все остальные — 25% прочности.
        if (data.hasProfession(Profession.BLACKSMITH)) return;

        int maxDamage = stack.getMaxDamage();
        stack.setDamageValue((int) (maxDamage * 0.75));
    }
}