package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class FarmerBreedingRestrictionHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Нас интересуют только животные
        if (!(event.getTarget() instanceof Animal animal)) return;

        // Проверяем, что предмет в руке — еда для животного
        ItemStack stack = event.getItemStack();
        if (!animal.isFood(stack)) return;

        // Проверяем роль
        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (data.hasProfession(Profession.FARMER_COOK)) return;

        // Не Фермер — запрещаем кормление (и, следовательно, разведение)
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);

        player.displayClientMessage(
                Component.literal("§cТолько Фермер может разводить животных!"),
                true
        );
    }
}