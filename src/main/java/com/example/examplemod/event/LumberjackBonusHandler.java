package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class LumberjackBonusHandler {

    private static final ResourceLocation BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "lumberjack_bonus");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;

        AttributeInstance attr = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (attr == null) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        boolean isLumberjack = data.hasProfession(Profession.LUMBERJACK);
        boolean hasBonus = attr.getModifier(BONUS_ID) != null;

        if (isLumberjack && !hasBonus) {
            attr.addTransientModifier(new AttributeModifier(
                    BONUS_ID,
                    0.2,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        } else if (!isLumberjack && hasBonus) {
            attr.removeModifier(BONUS_ID);
        }
    }
}