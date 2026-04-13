package com.kewz4.keepitems;

import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.OnDropCallback;

/**
 * Registers with the Accessories API so that kept items worn in accessory slots
 * (bracelets, rings, gloves, etc.) are returned to the player on death instead
 * of being dropped.
 *
 * This class is intentionally kept separate from KeepItemsMod so it is only
 * class-loaded when the Accessories mod is actually present on the server.
 */
public class AccessoriesIntegration {

    public static void register() {
        OnDropCallback.EVENT.register((dropRule, stack, slotReference, damageSource) -> {
            if (KeepItemsMod.isKeptItem(stack)) {
                return DropRule.KEEP;
            }
            // Return the existing drop rule unchanged for all other items
            return dropRule;
        });
    }
}
