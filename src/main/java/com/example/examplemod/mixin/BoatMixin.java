package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.profession.PlayerProfessionData;
import com.example.examplemod.profession.Profession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public class BoatMixin {

    private static final double MAX_SPEED = 0.55;

    @Inject(method = "controlBoat", at = @At("TAIL"))
    private void examplemod$fishermanBoatSpeed(CallbackInfo ci) {
        Boat boat = (Boat) (Object) this;

        if (!(boat.getControllingPassenger() instanceof Player player)) return;

        PlayerProfessionData data = player.getData(ExampleMod.PROFESSION_DATA);
        if (!data.hasProfession(Profession.FISHERMAN)) return;

        var motion = boat.getDeltaMovement();
        double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        // Ограничиваем максимальную скорость
        if (speed > MAX_SPEED) {
            double scale = MAX_SPEED / speed;
            boat.setDeltaMovement(motion.x * scale, motion.y, motion.z * scale);
        } else {
            // Мягкое ускорение до MAX_SPEED
            boat.setDeltaMovement(motion.x * 1.05, motion.y, motion.z * 1.05);
        }
    }
}