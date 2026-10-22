package com.example.examplemod.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public class CreateCompat {
    public static boolean isCreateItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals("create");
    }
}