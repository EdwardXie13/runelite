package net.runelite.client.plugins.pressSpace;

import lombok.experimental.UtilityClass;
import net.runelite.api.ItemID;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@UtilityClass
public class Potions {
    public static final List<Set<Pair<Integer, Integer>>> potionRecipe = new ArrayList<>(
        Arrays.asList(
            // unf pot + ingredient
            Set.of(Pair.of(ItemID.GUAM_POTION_UNF, 14), Pair.of(ItemID.EYE_OF_NEWT, 14)),
            Set.of(Pair.of(ItemID.HARRALANDER_POTION_UNF, 27), Pair.of(ItemID.VOLCANIC_ASH, 1)),
            Set.of(Pair.of(ItemID.HARRALANDER_POTION_UNF, 14), Pair.of(ItemID.CHOCOLATE_DUST, 14)),
            Set.of(Pair.of(ItemID.TARROMIN_POTION_UNF, 14), Pair.of(ItemID.LIMPWURT_ROOT, 14)),
            Set.of(Pair.of(ItemID.TARROMIN_POTION_UNF, 14), Pair.of(ItemID.ASHES, 14)),
            Set.of(Pair.of(ItemID.RANARR_POTION_UNF, 14), Pair.of(ItemID.SNAPE_GRASS, 14)),
            Set.of(Pair.of(ItemID.TOADFLAX_POTION_UNF, 14), Pair.of(ItemID.CRUSHED_NEST, 14)),
            Set.of(Pair.of(ItemID.IRIT_POTION_UNF, 14), Pair.of(ItemID.EYE_OF_NEWT, 14)),
            Set.of(Pair.of(ItemID.IRIT_POTION_UNF, 14), Pair.of(ItemID.UNICORN_HORN_DUST, 14)),
            Set.of(Pair.of(ItemID.AVANTOE_POTION_UNF, 14), Pair.of(ItemID.MORT_MYRE_FUNGUS, 14)),
            Set.of(Pair.of(ItemID.AVANTOE_POTION_UNF, 14), Pair.of(ItemID.SNAPE_GRASS, 14)),
            Set.of(Pair.of(ItemID.KWUARM_POTION_UNF, 14), Pair.of(ItemID.LIMPWURT_ROOT, 14)),
            Set.of(Pair.of(ItemID.SNAPDRAGON_POTION_UNF, 14), Pair.of(ItemID.RED_SPIDERS_EGGS, 14)),
            Set.of(Pair.of(ItemID.CADANTINE_POTION_UNF, 14), Pair.of(ItemID.WHITE_BERRIES, 14)),
            Set.of(Pair.of(ItemID.LANTADYME_POTION_UNF, 14), Pair.of(ItemID.DRAGON_SCALE_DUST, 14)),
            Set.of(Pair.of(ItemID.LANTADYME_POTION_UNF, 14), Pair.of(ItemID.POTATO_CACTUS, 14)),
            Set.of(Pair.of(ItemID.DWARF_WEED_POTION_UNF, 14), Pair.of(ItemID.WINE_OF_ZAMORAK, 14)),
            Set.of(Pair.of(ItemID.DWARF_WEED_POTION_UNF, 14), Pair.of(ItemID.NIHIL_DUST, 14)),
            Set.of(Pair.of(ItemID.TORSTOL_POTION_UNF, 14), Pair.of(ItemID.JANGERBERRIES, 14)),

            // pot + ingredient
            Set.of(Pair.of(ItemID.SUPER_ENERGY3, 27), Pair.of(ItemID.AMYLASE_CRYSTAL, 1)),
            Set.of(Pair.of(ItemID.SUPER_ENERGY4, 27), Pair.of(ItemID.AMYLASE_CRYSTAL, 1)),
            Set.of(Pair.of(ItemID.ANTIFIRE_POTION4, 14), Pair.of(ItemID.CRUSHED_SUPERIOR_DRAGON_BONES, 14)),

            // super combat
            Set.of(Pair.of(ItemID.TORSTOL, 7),
                    Pair.of(ItemID.SUPER_ATTACK4, 7),
                    Pair.of(ItemID.SUPER_DEFENCE4, 7),
                    Pair.of(ItemID.SUPER_STRENGTH4, 7)),

            // quick close bank
            Set.of(Pair.of(ItemID.CHOCOLATE_BAR, 27), Pair.of(ItemID.PESTLE_AND_MORTAR, 1)),
            Set.of(Pair.of(ItemID.BIRD_NEST_5075, 27), Pair.of(ItemID.PESTLE_AND_MORTAR, 1)),
            Set.of(Pair.of(ItemID.BUCKET_OF_SAND, 14), Pair.of(ItemID.SODA_ASH, 14)),
            Set.of(Pair.of(ItemID.MOLTEN_GLASS, 27), Pair.of(ItemID.GLASSBLOWING_PIPE, 1)),
            Set.of(Pair.of(ItemID.BATTLESTAFF, 14), Pair.of(ItemID.FIRE_ORB, 14)),

//            Set.of(Pair.of(ItemID.GREEN_DRAGON_LEATHER, 26),Pair.of(ItemID.THREAD, 1), Pair.of(ItemID.NEEDLE, 1)),
//            Set.of(Pair.of(ItemID.BLUE_DRAGON_LEATHER, 26),Pair.of(ItemID.THREAD, 1), Pair.of(ItemID.NEEDLE, 1)),
//            Set.of(Pair.of(ItemID.RED_DRAGON_LEATHER, 26),Pair.of(ItemID.THREAD, 1), Pair.of(ItemID.NEEDLE, 1)),
//            Set.of(Pair.of(ItemID.BLACK_DRAGON_LEATHER, 26),Pair.of(ItemID.THREAD, 1), Pair.of(ItemID.NEEDLE, 1)),

            Set.of(Pair.of(ItemID.GREEN_DRAGONHIDE, 25),Pair.of(ItemID.NATURE_RUNE, 1), Pair.of(ItemID.ASTRAL_RUNE, 1), Pair.of(ItemID.JUG ,1)),
            Set.of(Pair.of(ItemID.BLUE_DRAGONHIDE, 25),Pair.of(ItemID.NATURE_RUNE, 1), Pair.of(ItemID.ASTRAL_RUNE, 1), Pair.of(ItemID.JUG ,1)),
            Set.of(Pair.of(ItemID.RED_DRAGONHIDE, 25),Pair.of(ItemID.NATURE_RUNE, 1), Pair.of(ItemID.ASTRAL_RUNE, 1), Pair.of(ItemID.JUG ,1)),
            Set.of(Pair.of(ItemID.BLACK_DRAGONHIDE, 25),Pair.of(ItemID.NATURE_RUNE, 1), Pair.of(ItemID.ASTRAL_RUNE, 1), Pair.of(ItemID.JUG ,1)),

            Set.of(Pair.of(ItemID.UNCUT_EMERALD, 27),Pair.of(ItemID.CHISEL, 1)),
            Set.of(Pair.of(ItemID.UNCUT_SAPPHIRE, 27),Pair.of(ItemID.CHISEL, 1)),
            Set.of(Pair.of(ItemID.UNCUT_RUBY, 27),Pair.of(ItemID.CHISEL, 1)),
            Set.of(Pair.of(ItemID.UNCUT_DIAMOND, 27),Pair.of(ItemID.CHISEL, 1)),

            Set.of(Pair.of(ItemID.TEAK_LOGS, 27),Pair.of(ItemID.COINS_995, 1)),
            Set.of(Pair.of(ItemID.SILVER_BAR, 26), Pair.of(ItemID.SILVER_BOLTS_UNF, 1), Pair.of(ItemID.BOLT_MOULD, 1)),
            Set.of(Pair.of(ItemID.SILVER_BAR, 27), Pair.of(ItemID.BOLT_MOULD, 1)),
            Set.of(Pair.of(ItemID.SEAWEED, 28)),
            Set.of(Pair.of(ItemID.JUG, 28)),
            Set.of(Pair.of(ItemID.JUG_OF_WATER, 14), Pair.of(ItemID.GRAPES, 14)),
            Set.of(Pair.of(ItemID.CAKE_TIN, 6), Pair.of(ItemID.BUCKET_OF_MILK, 6), Pair.of(ItemID.POT_OF_FLOUR, 6), Pair.of(ItemID.EGG, 6)),
            Set.of(Pair.of(ItemID.UNCOOKED_CAKE, 6))
        )
    );
}
