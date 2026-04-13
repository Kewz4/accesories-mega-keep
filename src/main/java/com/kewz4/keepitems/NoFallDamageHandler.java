package com.kewz4.keepitems;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NoFallDamageHandler {

    private static final long GRACE_PERIOD_MS = 15_000L;

    /** UUID → time of dismount in milliseconds. */
    private static final Map<UUID, Long> DISMOUNT_TIMES = new ConcurrentHashMap<>();

    /** Called by the mixin when a player stops riding an entity. */
    public static void onDismount(ServerPlayerEntity player) {
        DISMOUNT_TIMES.put(player.getUuid(), System.currentTimeMillis());
    }

    /** Called on respawn/disconnect so stale entries don't linger. */
    public static void clear(ServerPlayerEntity player) {
        DISMOUNT_TIMES.remove(player.getUuid());
    }

    private static boolean isInGracePeriod(ServerPlayerEntity player) {
        Long dismountTime = DISMOUNT_TIMES.get(player.getUuid());
        if (dismountTime == null) return false;
        if (System.currentTimeMillis() - dismountTime > GRACE_PERIOD_MS) {
            DISMOUNT_TIMES.remove(player.getUuid());
            return false;
        }
        return true;
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player
                    && source.isOf(DamageTypes.FALL)
                    && isInGracePeriod(player)) {
                return false; // Cancel fall damage during grace period
            }
            return true;
        });
    }
}
