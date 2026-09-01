package net.runelite.client.plugins.vorkathAuto;

import net.runelite.api.ItemID;

import java.util.Set;

/**
 * Loot-related constants for the Vorkath fight.
 *
 * Prices are NOT stored here — they change daily and are pulled live at pickup
 * time from RuneLite's ItemManager (own inventory) and the GroundItemsPlugin
 * cache (ground stacks, which already carries gePrice / haPrice / stackable).
 * This file is purely a small set of identifier constants the ranking logic
 * needs — mandatory drops, protected inventory items, etc.
 */
public final class VorkathAutoLoot {
    private VorkathAutoLoot() {}

    /** Guaranteed Vorkath drop, unstackable — 2 per kill = 2 inventory slots. Always picked up. */
    public static final int SUPERIOR_DRAGON_BONES = ItemID.SUPERIOR_DRAGON_BONES;

    /** Guaranteed Vorkath drop, unnoted here — 2 per kill = 2 slots. NOT always-pick;
     *  ranks against other loot by gePricePerSlot like everything else. */
    public static final int BLUE_DRAGONHIDE = ItemID.BLUE_DRAGONHIDE;

    /**
     * Inventory items that must NEVER be dropped to make room for loot.
     * The user's setup carries exactly one rune pouch and one slayer staff.
     */
    public static final Set<Integer> PROTECTED_ITEM_IDS = Set.of(
        ItemID.RUNE_POUCH,       // regular rune pouch (12791)
        ItemID.SLAYERS_STAFF     // slayer staff (4170)
    );

    /**
     * Loot decision mode — set by canContinueFight() + hasEnoughSupplies() gates.
     *
     *   CONSERVATIVE: we still plan to keep killing. Respect MIN_* supply floors.
     *   AGGRESSIVE:   we're going to TP out anyway. Drop anything except protected
     *                 items to fit the last-run haul.
     */
    public enum LootMode { CONSERVATIVE, AGGRESSIVE }
}
