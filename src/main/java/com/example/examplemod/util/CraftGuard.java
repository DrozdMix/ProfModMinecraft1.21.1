package com.example.examplemod.util;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.compat.CreateCompat;
import com.example.examplemod.compat.ModCompat;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;

public final class CraftGuard {
    private CraftGuard() {}

    public static boolean isBuildingBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();

        // Формы и конструкции
        if (block instanceof StairBlock) return true;
        if (block instanceof SlabBlock) return true;
        if (block instanceof FenceBlock) return true;
        if (block instanceof FenceGateBlock) return true;
        if (block instanceof DoorBlock) return true;
        if (block instanceof TrapDoorBlock) return true;
        if (block instanceof WallBlock) return true;
        if (block instanceof IronBarsBlock) return true;  // решётки + стеклянные панели
        if (block instanceof ChainBlock) return true;

        // Механизмы и редстоун-компоненты
        if (block instanceof ButtonBlock) return true;
        if (block instanceof LeverBlock) return true;
        if (block instanceof PressurePlateBlock) return true;
        if (block instanceof WeightedPressurePlateBlock) return true;
        if (block instanceof RedStoneWireBlock) return true;
        if (block instanceof RedstoneTorchBlock) return true;
        if (block instanceof RepeaterBlock) return true;
        if (block instanceof ComparatorBlock) return true;
        if (block instanceof ObserverBlock) return true;
        if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) return true;
        if (block instanceof DispenserBlock) return true;
        if (block instanceof DropperBlock) return true;
        if (block instanceof HopperBlock) return true;
        if (block instanceof NoteBlock) return true;
        if (block instanceof TripWireBlock) return true;
        if (block instanceof TripWireHookBlock) return true;
        if (block instanceof TargetBlock) return true;
        if (block instanceof LightningRodBlock) return true;
        if (block instanceof DaylightDetectorBlock) return true;
        if (block instanceof SculkSensorBlock) return true;

        // Декор и базовые строительные блоки
        return block == Blocks.STONE_BRICKS
                || block == Blocks.MOSSY_STONE_BRICKS
                || block == Blocks.CRACKED_STONE_BRICKS
                || block == Blocks.CHISELED_STONE_BRICKS
                || block == Blocks.BRICKS
                || block == Blocks.QUARTZ_BLOCK
                || block == Blocks.CHISELED_QUARTZ_BLOCK
                || block == Blocks.QUARTZ_PILLAR
                || block == Blocks.SMOOTH_QUARTZ
                || block == Blocks.QUARTZ_BRICKS
                || block == Blocks.GLASS
                || block == Blocks.GLASS_PANE
                || block == Blocks.TINTED_GLASS;
    }

    public static boolean isTakeAllowed(Player player, ItemStack result) {
        if (result.isEmpty()) return true;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);

        if (ModCompat.isCreateLoaded() && CreateCompat.isCreateItem(result)
                && !data.hasProfession(Profession.BUILDER)) {
            return false;
        }

        return !isBuildingBlock(result) || data.hasProfession(Profession.BUILDER);
    }
}