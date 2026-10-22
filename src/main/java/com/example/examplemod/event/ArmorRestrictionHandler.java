package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ArmorRestrictionHandler {

    /**
     * Тяжёлая броня — та, что не кожа и не кольчуга.
     * Исключения:
     *  - Железный нагрудник — доступен всем
     *  - Броня из Create и производных модов — доступна всем
     */
    public static boolean isHeavyArmor(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armor)) return false;

        // Железный нагрудник — всем
        if (stack.is(Items.IRON_CHESTPLATE)) return false;

        // Броня из Create и производных модов — всем
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String ns = key.getNamespace();
        if (ns.equals("create")
                || ns.equals("flywheel")
                || ns.equals("ponder")
                || ns.equals("createaddition")
                || ns.equals("createbigcannons")
                || ns.equals("createmetallurgy")
                || ns.equals("create_enchantment_industry")
                || ns.equals("create_connected")
                || ns.equals("create_sa")) {
            return false;
        }

        var materialHolder = armor.getMaterial();

        return !materialHolder.is(ArmorMaterials.LEATHER)
                && !materialHolder.is(ArmorMaterials.CHAIN);
    }

    public static boolean canWear(Player player) {
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        return data.hasProfession(Profession.WARRIOR)
                || data.hasProfession(Profession.MINER);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        EquipmentSlot slot = event.getSlot();
        if (!slot.isArmor()) return;

        ItemStack newStack = event.getTo();
        if (!isHeavyArmor(newStack)) return;

        if (canWear(player)) return;

        // Подстраховка: возвращаем старый предмет БЕЗ копирования, чтобы не было дюпа
        ItemStack oldStack = event.getFrom();
        player.setItemSlot(slot, oldStack);

        if (!player.getInventory().add(newStack)) {
            player.drop(newStack, false);
        }

        player.displayClientMessage(
                Component.literal("§cЭта броня доступна только Воину и Шахтёру!"),
                true
        );
    }
}