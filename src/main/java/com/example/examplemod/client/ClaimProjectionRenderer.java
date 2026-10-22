package com.example.examplemod.client;

import com.example.examplemod.network.ClientPacketHandler;
import com.example.examplemod.network.RegionBox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class ClaimProjectionRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ClientPacketHandler.activeRegions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        for (RegionBox box : ClientPacketHandler.activeRegions) {
            AABB aabb = new AABB(
                    box.min().getX(), box.min().getY(), box.min().getZ(),
                    box.max().getX() + 1, box.max().getY() + 1, box.max().getZ() + 1
            );
            LevelRenderer.renderLineBox(poseStack, consumer, aabb, 1.0f, 0.0f, 0.0f, 1.0f);
        }

        buffer.endBatch(RenderType.lines());
        poseStack.popPose();
    }
}