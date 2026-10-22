package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    private static boolean isOre(ItemStack stack) {
        return stack.is(Items.RAW_IRON) || stack.is(Items.RAW_GOLD) || stack.is(Items.RAW_COPPER)
                || stack.is(Items.IRON_ORE) || stack.is(Items.GOLD_ORE) || stack.is(Items.COPPER_ORE)
                || stack.is(Items.DEEPSLATE_IRON_ORE) || stack.is(Items.DEEPSLATE_GOLD_ORE)
                || stack.is(Items.DEEPSLATE_COPPER_ORE);
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void examplemod$fixOreInFurnace(int slotId, int button, ClickType clickType,
                                            Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (!(menu instanceof AbstractFurnaceMenu)) return;

        if (menu.slots.isEmpty()) return;
        ItemStack stackInSlot0 = menu.slots.get(0).getItem();
        if (!isOre(stackInSlot0)) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.BLACKSMITH)) return;

        ItemStack toReturn = stackInSlot0.copy();
        menu.slots.get(0).set(ItemStack.EMPTY);

        if (!player.getInventory().add(toReturn)) {
            player.drop(toReturn, false);
        }

        menu.broadcastFullState();
    }
}