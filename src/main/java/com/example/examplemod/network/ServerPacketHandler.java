package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.claim.ClaimManager;
import com.example.examplemod.claim.ClaimRegion;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPacketHandler {

    /** Максимальная дистанция (в блоках), с которой можно "подобрать" животное. */
    private static final double MAX_REACH_SQR = 6.0 * 6.0;

    public static void handlePickupAnimal(PickupAnimalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(packet.animalUuid());

            if (!(entity instanceof Animal animal)) return;

            // Защита от подделанного пакета: животное должно реально быть рядом с игроком
            // (level.getEntity ищет только в пределах level, так что дистанция — единственное, что нужно проверить).
            if (player.distanceToSqr(animal) > MAX_REACH_SQR) return;

            if (animal.isBaby()) {
                player.displayClientMessage(
                        Component.literal("§cМожно переносить только взрослых животных!"), true);
                return;
            }

            PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
            if (!data.hasProfession(Profession.FARMER_COOK)) {
                player.displayClientMessage(
                        Component.literal("§cТолько Фермер может переносить животных!"), true);
                return;
            }

            // Нельзя забирать чужих животных с приватной территории.
            ClaimManager manager = ClaimManager.get(level);
            ClaimRegion region = manager.getRegionAt(level, animal.blockPosition());
            if (region != null && !region.canBuild(player.getUUID()) && !data.hasProfession(Profession.MAYOR)) {
                player.displayClientMessage(
                        Component.literal("§cЭто животное находится на чужой защищённой территории!"), true);
                return;
            }

            // Получаем спавн-яйцо
            SpawnEggItem egg = SpawnEggItem.byId(animal.getType());
            if (egg == null) {
                player.displayClientMessage(
                        Component.literal("§cЭто животное нельзя перенести."), true);
                return;
            }

            // Выдаём яйцо
            ItemStack eggStack = new ItemStack(egg);
            if (!player.getInventory().add(eggStack)) {
                player.drop(eggStack, false);
            }

            // Удаляем животное
            animal.discard();

            player.displayClientMessage(
                    Component.literal("§aЖивотное перенесено в яйцо."), true);
        });
    }
}