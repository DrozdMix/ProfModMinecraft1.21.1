package com.example.examplemod.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FarmersDelightCompat {
    public static boolean isFDCookingBlock(BlockState state) {
        Block block = state.getBlock();
        var key = BuiltInRegistries.BLOCK.getKey(block);
        String path = key.getPath();

        return key.getNamespace().equals("farmersdelight")
                && (path.equals("cooking_pot")
                || path.equals("skillet")
                || path.equals("stove")
                || path.equals("cutting_board")
                || path.equals("basket"));
    }
}