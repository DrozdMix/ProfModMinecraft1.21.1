package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Shadow private int[] costs;

    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void examplemod$discountForEnchanter(net.minecraft.world.Container container, CallbackInfo ci) {
        EnchantmentMenu self = (EnchantmentMenu) (Object) this;
        Player player = null;
        for (var slot : self.slots) {
            if (slot.container instanceof Inventory inventory) {
                player = inventory.player;
                break;
            }
        }
        if (player == null) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (!data.hasProfession(Profession.ENCHANTER_ALCHEMIST)) return;

        for (int i = 0; i < costs.length; i++) {
            if (costs[i] > 0) {
                costs[i] = Math.max(1, costs[i] / 2);
            }
        }
    }
}