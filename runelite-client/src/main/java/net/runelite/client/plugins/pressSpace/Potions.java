package net.runelite.client.plugins.pressSpace;

import lombok.experimental.UtilityClass;
import net.runelite.api.ItemID;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class Potions {
    public static final List<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> potionRecipe = new ArrayList<>(
        Arrays.asList(
            // unf pot + ingredient
            Pair.of(Pair.of(ItemID.GUAM_POTION_UNF, 14), Pair.of(ItemID.EYE_OF_NEWT, 14)),
            Pair.of(Pair.of(ItemID.TARROMIN_POTION_UNF, 14), Pair.of(ItemID.LIMPWURT_ROOT, 14)),
            Pair.of(Pair.of(ItemID.TARROMIN_POTION_UNF, 14), Pair.of(ItemID.ASHES, 14)),
            Pair.of(Pair.of(ItemID.RANARR_POTION_UNF, 14), Pair.of(ItemID.SNAPE_GRASS, 14)),
            Pair.of(Pair.of(ItemID.TOADFLAX_POTION_UNF, 14), Pair.of(ItemID.CRUSHED_NEST, 14)),
            Pair.of(Pair.of(ItemID.IRIT_POTION_UNF, 14), Pair.of(ItemID.EYE_OF_NEWT, 14)),
            Pair.of(Pair.of(ItemID.IRIT_POTION_UNF, 14), Pair.of(ItemID.UNICORN_HORN_DUST, 14)),
            Pair.of(Pair.of(ItemID.AVANTOE_POTION_UNF, 14), Pair.of(ItemID.MORT_MYRE_FUNGUS, 14)),
            Pair.of(Pair.of(ItemID.KWUARM_POTION_UNF, 14), Pair.of(ItemID.LIMPWURT_ROOT, 14)),
            Pair.of(Pair.of(ItemID.SNAPDRAGON_POTION_UNF, 14), Pair.of(ItemID.RED_SPIDERS_EGGS, 14)),
            Pair.of(Pair.of(ItemID.CADANTINE_POTION_UNF, 14), Pair.of(ItemID.WHITE_BERRIES, 14)),
            Pair.of(Pair.of(ItemID.CADANTINE_POTION_UNF, 14), Pair.of(ItemID.WINE_OF_ZAMORAK, 14)),
            Pair.of(Pair.of(ItemID.CADANTINE_POTION_UNF, 14), Pair.of(ItemID.POTATO_CACTUS, 14)),
            Pair.of(Pair.of(ItemID.LANTADYME_POTION_UNF, 14), Pair.of(ItemID.DRAGON_SCALE_DUST, 14)),
            Pair.of(Pair.of(ItemID.DWARF_WEED_POTION_UNF, 14), Pair.of(ItemID.WINE_OF_ZAMORAK, 14)),
            Pair.of(Pair.of(ItemID.DWARF_WEED_POTION_UNF, 14), Pair.of(ItemID.NIHIL_DUST, 14)),
            Pair.of(Pair.of(ItemID.TORSTOL_POTION_UNF, 14), Pair.of(ItemID.JANGERBERRIES, 14)),

            // pot + ingredient
            Pair.of(Pair.of(ItemID.SUPER_ENERGY3, 27), Pair.of(ItemID.AMYLASE_CRYSTAL, 1)),
            Pair.of(Pair.of(ItemID.SUPER_ENERGY4, 27), Pair.of(ItemID.AMYLASE_CRYSTAL, 1)),
            Pair.of(Pair.of(ItemID.ANTIFIRE_POTION4, 14), Pair.of(ItemID.CRUSHED_SUPERIOR_DRAGON_BONES, 14))
        )
    );
}
