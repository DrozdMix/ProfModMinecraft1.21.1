package com.example.examplemod.client;

import com.example.examplemod.network.PickupAnimalPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class AnimalPickupHandler {

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        // СКМ = GLFW_MOUSE_BUTTON_MIDDLE
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Проверяем, что под прицелом — животное
        if (!(mc.hitResult instanceof EntityHitResult entityHit)) return;

        Entity target = entityHit.getEntity();
        if (!(target instanceof Animal animal)) return;
        if (animal.isBaby()) return;

        // Отправляем пакет на сервер
        PacketDistributor.sendToServer(new PickupAnimalPacket(animal.getUUID()));
    }
}