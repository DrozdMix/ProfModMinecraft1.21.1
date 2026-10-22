package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin extends AbstractContainerMenu {

    protected AbstractFurnaceMenuMixin() {
        super(null, 0);
    }

    private static boolean isOre(ItemStack stack) {
        return stack.is(Items.RAW_IRON) || stack.is(Items.RAW_GOLD) || stack.is(Items.RAW_COPPER)
                || stack.is(Items.IRON_ORE) || stack.is(Items.GOLD_ORE) || stack.is(Items.COPPER_ORE)
                || stack.is(Items.DEEPSLATE_IRON_ORE) || stack.is(Items.DEEPSLATE_GOLD_ORE)
                || stack.is(Items.DEEPSLATE_COPPER_ORE);
    }

    /**
     * ВАЖНО: раньше владелец печи запоминался в одном статическом поле
     * (FurnaceContext) на весь сервер — если печь одновременно открывали
     * два игрока, состояние путалось и ограничение могло сработать не на
     * того игрока (или не сработать вовсе). Теперь владелец захватывается
     * прямо из слотов этого конкретного меню (слоты инвентаря игрока,
     * добавленные после трёх слотов печи, ссылаются на его Inventory) и
     * передаётся в конструктор слота напрямую — никакого общего состояния.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void examplemod$replaceInputSlot(CallbackInfo ci) {
        if (this.slots.isEmpty()) return;
        Slot original = this.slots.get(0);
        Player owner = examplemod$findOwner();
        this.slots.set(0, new RestrictedFurnaceInputSlot(original.container, original.index,
                original.x, original.y, owner));
    }

    private Player examplemod$findOwner() {
        for (Slot slot : this.slots) {
            if (slot.container instanceof Inventory inventory) {
                return inventory.player;
            }
        }
        return null;
    }

    private static class RestrictedFurnaceInputSlot extends Slot {
        private final Player owner;

        public RestrictedFurnaceInputSlot(Container container, int index, int x, int y, Player owner) {
            super(container, index, x, y);
            this.owner = owner;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isOre(stack)) return super.mayPlace(stack);
            if (owner == null) return super.mayPlace(stack);

            PlayerProfessionData data = owner.getData(ExampleMod.PROFESSION_DATA);
            if (data.hasProfession(Profession.BLACKSMITH)) return super.mayPlace(stack);

            return false;
        }
    }
}
