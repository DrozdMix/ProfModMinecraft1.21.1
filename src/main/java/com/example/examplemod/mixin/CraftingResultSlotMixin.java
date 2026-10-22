package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class CraftingResultSlotMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void examplemod$onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide()) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);

        if (!stack.isDamageableItem()) return;

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