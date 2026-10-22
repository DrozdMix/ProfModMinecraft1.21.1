package com.example.examplemod.compat;

import net.neoforged.fml.ModList;

public class ModCompat {
    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }

    public static boolean isFarmersDelightLoaded() {
        return ModList.get().isLoaded("farmersdelight");
    }
}