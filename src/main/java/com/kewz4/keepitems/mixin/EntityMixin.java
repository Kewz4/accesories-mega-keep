package com.kewz4.keepitems.mixin;

import com.kewz4.keepitems.NoFallDamageHandler;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * Injects at the very start of stopRiding(), before the vehicle reference
     * is cleared, so getVehicle() still returns the entity being dismounted.
     */
    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void onStopRiding(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayerEntity player && player.getVehicle() != null) {
            NoFallDamageHandler.onDismount(player);
        }
    }
}
