package net.runelite.client.plugins.smithing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnvilOptions {
    NONE("DNE"),
    DAGGER("dagger"),
    AXE("ace"),
    CHAINBODY("chainbody"),
    MED_HELM("med helm"),
    DART_TIP("dart tip"),
    BOLTS("bolts"),
    SWORD("sword"),
    MACE("mace"),
    PLATELEGS("platelegs"),
    FULL_HELM("full helm"),
    ARROWTIPS("arrowtips"),
    LIMBS("limbs"),
    SCIMITAR("scimitar"),
    WARHAMMER("warhammer"),
    PLATESKIRT("plateskirt"),
    SQ_SHIELD("sq shield"),
    KNIVES("knife"),
    LONGSWORD("longsword"),
    BATTLEAXE("battleaxe"),
    PLATEBODY("platebody"),
    KITESHIELD("kiteshield"),
    GRAPPLE_TIP("grapple tip"),
    TWO_HAND_SWORD("2h sword"),
    CLAWS("claws"),
    NAILS("nails"),
    JAVELIN_HEADS("javelin heads");

    private final String option;
}
