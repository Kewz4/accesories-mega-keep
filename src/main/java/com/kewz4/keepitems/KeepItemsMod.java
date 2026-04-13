package com.kewz4.keepitems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class KeepItemsMod implements ModInitializer {

    public static final String MOD_ID = "keep_mega_items";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Items that must be kept on player death.
     * These cover both regular inventory slots and accessory slots.
     */
    public static final Set<String> KEEP_ITEMS = Set.of(
        "mega_showdown:mega_bracelet",
        "mega_showdown:mega_bracelet_blue",
        "mega_showdown:mega_bracelet_red",
        "mega_showdown:mega_bracelet_black",
        "mega_showdown:mega_bracelet_green",
        "mega_showdown:mega_bracelet_yellow",
        "mega_showdown:mega_bracelet_pink",
        "mega_showdown:may_bracelet",
        "mega_showdown:mega_ring",
        "mega_showdown:lysandre_ring",
        "mega_showdown:brendan_mega_cuff",
        "mega_showdown:korrina_glove",
        "mega_showdown:maxie_glasses",
        "mega_showdown:archie_anchor",
        "mega_showdown:lisia_mega_tiara",
        "mega_showdown:tera_orb",
        "mega_showdown:omni_ring",
        "mega_showdown:z_ring",
        "mega_showdown:z_ring_black",
        "mega_showdown:z_ring_yellow",
        "mega_showdown:z_ring_green",
        "mega_showdown:z_ring_blue",
        "mega_showdown:z_ring_pink",
        "mega_showdown:z_ring_red",
        "mega_showdown:z_power_ring",
        "mega_showdown:olivias_z_ring",
        "mega_showdown:olivia_z_power_ring",
        "mega_showdown:hapus_z_ring",
        "mega_showdown:hapus_z_power_ring",
        "mega_showdown:rocket_z_power_ring",
        "mega_showdown:gladion_z_power_ring",
        "mega_showdown:nanu_z_power_ring",
        "mega_showdown:dynamax_band"
    );

    /**
     * Temporary store for items rescued from inventory before death drop.
     * Keyed by player UUID, cleared on respawn.
     */
    static final Map<UUID, List<ItemStack>> PENDING_ITEMS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        registerInventoryDeathHandling();

        if (FabricLoader.getInstance().isModLoaded("accessories")) {
            AccessoriesIntegration.register();
            LOGGER.info("[{}] Accessories mod detected - accessory slot keep-on-death registered.", MOD_ID);
        } else {
            LOGGER.info("[{}] Accessories mod not detected - only handling regular inventory slots.", MOD_ID);
        }

        LOGGER.info("[{}] Mod initialized. Protecting {} items on death.", MOD_ID, KEEP_ITEMS.size());
    }

    /**
     * Handles keeping items that are in the player's regular inventory (including
     * hotbar, main inventory, armor slots, and offhand).
     *
     * Strategy:
     * 1. ALLOW_DEATH: fires right before the player actually dies. At this point
     *    the player's inventory is intact. We extract kept items and store them
     *    in PENDING_ITEMS, so dropInventory() never sees them.
     * 2. COPY_FROM (alive=false): fires when the new player entity is set up after
     *    death. We restore the saved items into the new player's inventory.
     */
    private void registerInventoryDeathHandling() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }

            List<ItemStack> toKeep = new ArrayList<>();
            var inventory = player.getInventory();

            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty() && isKeptItem(stack)) {
                    toKeep.add(stack.copy());
                    inventory.setStack(i, ItemStack.EMPTY);
                }
            }

            if (!toKeep.isEmpty()) {
                PENDING_ITEMS.put(player.getUuid(), toKeep);
                LOGGER.debug("[{}] Saved {} kept item(s) for {} before death.",
                    MOD_ID, toKeep.size(), player.getNameForScoreboard());
            }

            return true; // Always allow death to proceed
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                // Player is still alive (e.g. respawning to bed without dying) - no action needed
                return;
            }

            List<ItemStack> toRestore = PENDING_ITEMS.remove(oldPlayer.getUuid());
            if (toRestore == null || toRestore.isEmpty()) {
                return;
            }

            for (ItemStack stack : toRestore) {
                if (!newPlayer.getInventory().insertStack(stack)) {
                    // Inventory somehow full — drop at spawn point rather than silently losing the item
                    newPlayer.dropItem(stack, false);
                    LOGGER.warn("[{}] Could not restore {} to {}'s inventory; dropping at spawn.",
                        MOD_ID, stack.getItem(), newPlayer.getNameForScoreboard());
                }
            }

            LOGGER.debug("[{}] Restored {} kept item(s) to {} after respawn.",
                MOD_ID, toRestore.size(), newPlayer.getNameForScoreboard());
        });
    }

    /**
     * Returns true if this item should be kept on death.
     */
    public static boolean isKeptItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return KEEP_ITEMS.contains(Registries.ITEM.getId(stack.getItem()).toString());
    }
}
