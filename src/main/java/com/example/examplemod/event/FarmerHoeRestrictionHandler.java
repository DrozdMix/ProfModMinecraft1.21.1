package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class FarmerHoeRestrictionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Проверяем, что в руке мотыга
        if (!(event.getItemStack().getItem() instanceof HoeItem)) return;

        // Проверяем, что блок — земля или трава
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.GRASS_BLOCK)
                && !state.is(Blocks.DIRT)
                && !state.is(Blocks.COARSE_DIRT)
                && !state.is(Blocks.ROOTED_DIRT)) {
            return;
        }

        // Проверяем роль
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.FARMER_COOK)) {
            StaminaHandler.applyProfessionalExhaustion(player, 0.05f);
            return;
        }

        // Не Фермер — запрещаем вспашку
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);

        player.displayClientMessage(
                Component.literal("§cТолько Фермер может вспахивать землю!"),
                true
        );
    }
}