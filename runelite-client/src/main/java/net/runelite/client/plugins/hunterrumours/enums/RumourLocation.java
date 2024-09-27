package net.runelite.client.plugins.hunterrumours.enums;

import static net.runelite.client.plugins.hunterrumours.enums.Rumour.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
public enum RumourLocation
{
	TROPICAL_WAGTAIL_FELDIP_HUNTER_AREA_1(TROPICAL_WAGTAIL, "Feldip Hunter area", "AKS", new WorldPoint(2526, 2939, 0)),
	TROPICAL_WAGTAIL_FELDIP_HUNTER_AREA_2(TROPICAL_WAGTAIL, "Feldip Hunter area", "AKS", new WorldPoint(2511, 2914, 0)),
	TROPICAL_WAGTAIL_FELDIP_HUNTER_AREA_3(TROPICAL_WAGTAIL, "Feldip Hunter area", "AKS", new WorldPoint(2499, 2890, 0)),
	TROPICAL_WAGTAIL_FELDIP_HUNTER_AREA_4(TROPICAL_WAGTAIL, "Feldip Hunter area", "AKS", new WorldPoint(2545, 2882, 0)),

	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_1(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2307, 3577, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_2(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2362, 3578, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_3(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2350, 3563, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_4(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2316, 3560, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_5(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2330, 3552, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_6(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2342, 3544, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_7(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2306, 3541, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_8(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2363, 3540, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_9(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2315, 3523, 0)),
	WILD_KEBBIT_PISCATORIS_HUNTER_AREA_10(WILD_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2348, 3523, 0)),

	SAPPHIRE_GLACIALIS_RELLEKKA_HUNTER_AREA_1(SAPPHIRE_GLACIALIS, "Rellekka Hunter area", "DKS", new WorldPoint(2703, 3822, 0)),
	SAPPHIRE_GLACIALIS_RELLEKKA_HUNTER_AREA_2(SAPPHIRE_GLACIALIS, "Rellekka Hunter area", "DKS", new WorldPoint(2708, 3822, 0)),
	SAPPHIRE_GLACIALIS_RELLEKKA_HUNTER_AREA_3(SAPPHIRE_GLACIALIS, "Rellekka Hunter area", "DKS", new WorldPoint(2715, 3808, 0)),
	SAPPHIRE_GLACIALIS_RELLEKKA_HUNTER_AREA_4(SAPPHIRE_GLACIALIS, "Rellekka Hunter area", "DKS", new WorldPoint(2720, 3805, 0)),
	SAPPHIRE_GLACIALIS_RELLEKKA_HUNTER_AREA_5(SAPPHIRE_GLACIALIS, "Rellekka Hunter area", "DKS", new WorldPoint(2720, 3834, 0)),
	SAPPHIRE_GLACIALIS_RELLEKKA_HUNTER_AREA_6(SAPPHIRE_GLACIALIS, "Rellekka Hunter area", "DKS", new WorldPoint(2734, 3823, 0)),
	SAPPHIRE_GLACIALIS_FARMING_GUILD_1(SAPPHIRE_GLACIALIS, "Farming Guild", "CIR", new WorldPoint(1237, 3746, 0)),
	SAPPHIRE_GLACIALIS_FARMING_GUILD_2(SAPPHIRE_GLACIALIS, "Farming Guild", "CIR", new WorldPoint(1258, 3746, 0)),

	SWAMP_LIZARD_CANIFIS_HUNTER_AREA(SWAMP_LIZARD, "Canifis Hunter Area", "ALQ", new WorldPoint(3532, 3446, 0)),
	SWAMP_LIZARD_NORTH_WEST_OF_SLEPE(SWAMP_LIZARD, "North-west of Slepe", "", new WorldPoint(3684, 3403, 0)),

	SPINED_LARUPIA_FELDIP_HUNTER_AREA_1(SPINED_LARUPIA, "Feldip Hunter area", "AKS", new WorldPoint(2544, 2910, 0)),
	SPINED_LARUPIA_FELDIP_HUNTER_AREA_2(SPINED_LARUPIA, "Feldip Hunter area", "AKS", new WorldPoint(2550, 2904, 0)),
	SPINED_LARUPIA_FELDIP_HUNTER_AREA_3(SPINED_LARUPIA, "Feldip Hunter area", "AKS", new WorldPoint(2556, 2895, 0)),
	SPINED_LARUPIA_FELDIP_HUNTER_AREA_4(SPINED_LARUPIA, "Feldip Hunter area", "AKS", new WorldPoint(2563, 2888, 0)),
	SPINED_LARUPIA_FELDIP_HUNTER_AREA_5(SPINED_LARUPIA, "Feldip Hunter area", "AKS", new WorldPoint(2573, 2883, 0)),

	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_1(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2572, 2931, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_2(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2572, 2929, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_3(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2575, 2926, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_4(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2577, 2926, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_5(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2575, 2916, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_6(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2584, 2914, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_7(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2584, 2912, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_8(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2574, 2911, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_9(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2574, 2909, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_10(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2567, 2903, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_11(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2565, 2903, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_12(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2573, 2898, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_13(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2573, 2896, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_14(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2581, 2886, 0)),
	BARB_TAILED_KEBBIT_FELDIP_HUNTER_AREA_15(BARB_TAILED_KEBBIT, "Feldip Hunter area", "AKS", new WorldPoint(2581, 2884, 0)),

	SNOWY_KNIGHT_WEISS_1(SNOWY_KNIGHT, "Weiss", "", new WorldPoint(2864, 3957, 0)),
	SNOWY_KNIGHT_WEISS_2(SNOWY_KNIGHT, "Weiss", "", new WorldPoint(2870, 3955, 0)),
	SNOWY_KNIGHT_WEISS_3(SNOWY_KNIGHT, "Weiss", "", new WorldPoint(2867, 3954, 0)),
	SNOWY_KNIGHT_WEISS_4(SNOWY_KNIGHT, "Weiss", "", new WorldPoint(2874, 3952, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_UPPER_1(SNOWY_KNIGHT, "Rellekka Hunter area, Upper level", "DKS", new WorldPoint(2725, 3833, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_UPPER_2(SNOWY_KNIGHT, "Rellekka Hunter area, Upper level", "DKS", new WorldPoint(2708, 3815, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_UPPER_3(SNOWY_KNIGHT, "Rellekka Hunter area, Upper level", "DKS", new WorldPoint(2730, 3806, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_1(SNOWY_KNIGHT, "Rellekka Hunter area", "DKS", new WorldPoint(2701, 3804, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_2(SNOWY_KNIGHT, "Rellekka Hunter area", "DKS", new WorldPoint(2712, 3797, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_3(SNOWY_KNIGHT, "Rellekka Hunter area", "DKS", new WorldPoint(2737, 3792, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_4(SNOWY_KNIGHT, "Rellekka Hunter area", "DKS", new WorldPoint(2696, 3786, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_5(SNOWY_KNIGHT, "Rellekka Hunter area", "DKS", new WorldPoint(2737, 3778, 0)),
	SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_6(SNOWY_KNIGHT, "Rellekka Hunter area", "DKS", new WorldPoint(2714, 3777, 0)),
	SNOWY_KNIGHT_FARMING_GUILD_1(SNOWY_KNIGHT, "Farming Guild", "CIR", new WorldPoint(1238, 3740, 0)),
	SNOWY_KNIGHT_FARMING_GUILD_2(SNOWY_KNIGHT, "Farming Guild", "CIR", new WorldPoint(1223, 3723, 0)),

	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_1(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2321, 3644, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_2(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2321, 3643, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_3(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2308, 3642, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_4(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2310, 3642, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_5(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2342, 3641, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_6(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2343, 3641, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_7(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2327, 3635, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_8(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2328, 3635, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_9(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2336, 3632, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_10(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2336, 3631, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_11(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2323, 3628, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_12(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2324, 3628, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_13(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2307, 3621, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_14(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2307, 3620, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_15(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2323, 3614, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_16(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2324, 3614, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_17(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2307, 3606, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_18(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2307, 3605, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_19(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2319, 3595, 0)),
	PRICKLY_KEBBIT_PISCATORIS_HUNTER_AREA_20(PRICKLY_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2320, 3595, 0)),

	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_1(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1510, 3046, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_2(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1511, 3050, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_3(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1513, 3043, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_4(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1515, 3049, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_5(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1518, 3051, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_6(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1518, 3041, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_7(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1519, 3045, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_8(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1521, 3048, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_9(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1522, 3043, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_10(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1524, 3050, 0)),
	EMBERTAILED_JERBOA_WEST_OF_HUNTER_GUILD_11(EMBERTAILED_JERBOA, "West of Hunter Guild", "", new WorldPoint(1524, 3046, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_1(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1664, 2998, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_2(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1664, 3003, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_3(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1664, 3005, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_4(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1666, 3006, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_5(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1667, 2996, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_6(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1667, 3001, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_7(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1667, 3004, 0)),
	EMBERTAILED_JERBOA_NORTH_WEST_OF_THE_LOCUS_OASIS_8(EMBERTAILED_JERBOA, "North-west of the Locus Oasis", "AJP", new WorldPoint(1671, 3005, 0)),

	HORNED_GRAAHK_KARAMJA_HUNTER_AREA_1(HORNED_GRAAHK, "Karamja Hunter area", "CKR", new WorldPoint(2766, 3008, 0)),
	HORNED_GRAAHK_KARAMJA_HUNTER_AREA_2(HORNED_GRAAHK, "Karamja Hunter area", "CKR", new WorldPoint(2767, 3005, 0)),
	HORNED_GRAAHK_KARAMJA_HUNTER_AREA_3(HORNED_GRAAHK, "Karamja Hunter area", "CKR", new WorldPoint(2774, 3002, 0)),
	HORNED_GRAAHK_KARAMJA_HUNTER_AREA_4(HORNED_GRAAHK, "Karamja Hunter area", "CKR", new WorldPoint(2781, 3001, 0)),

	SPOTTED_KEBBIT_PISCATORIS_FALCONRY_AREA_1(SPOTTED_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2391, 3592, 0)),
	SPOTTED_KEBBIT_PISCATORIS_FALCONRY_AREA_2(SPOTTED_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2370, 3591, 0)),
	SPOTTED_KEBBIT_PISCATORIS_FALCONRY_AREA_3(SPOTTED_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2370, 3587, 0)),
	SPOTTED_KEBBIT_PISCATORIS_FALCONRY_AREA_4(SPOTTED_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2384, 3586, 0)),
	SPOTTED_KEBBIT_PISCATORIS_FALCONRY_AREA_5(SPOTTED_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2379, 3585, 0)),
	SPOTTED_KEBBIT_PISCATORIS_FALCONRY_AREA_6(SPOTTED_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2372, 3584, 0)),
	SPOTTED_KEBBIT_PISCATORIS_FALCONRY_AREA_7(SPOTTED_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2382, 3584, 0)),

	BLACK_WARLOCK_FELDIP_HUNTER_AREA_1(BLACK_WARLOCK, "Feldip Hunter area", "AKS", new WorldPoint(2563, 2920, 0)),
	BLACK_WARLOCK_FELDIP_HUNTER_AREA_2(BLACK_WARLOCK, "Feldip Hunter area", "AKS", new WorldPoint(2551, 2915, 0)),
	BLACK_WARLOCK_FELDIP_HUNTER_AREA_3(BLACK_WARLOCK, "Feldip Hunter area", "AKS", new WorldPoint(2540, 2914, 0)),
	BLACK_WARLOCK_FELDIP_HUNTER_AREA_4(BLACK_WARLOCK, "Feldip Hunter area", "AKS", new WorldPoint(2532, 2905, 0)),
	BLACK_WARLOCK_FELDIP_HUNTER_AREA_5(BLACK_WARLOCK, "Feldip Hunter area", "AKS", new WorldPoint(2540, 2898, 0)),
	BLACK_WARLOCK_FELDIP_HUNTER_AREA_6(BLACK_WARLOCK, "Feldip Hunter area", "AKS", new WorldPoint(2550, 2893, 0)),
	BLACK_WARLOCK_FELDIP_HUNTER_AREA_7(BLACK_WARLOCK, "Feldip Hunter area", "AKS", new WorldPoint(2566, 2886, 0)),
	BLACK_WARLOCK_IN_AND_AROUND_THE_FARMING_GUILD_1(BLACK_WARLOCK, "In and around the Farming Guild", "CIR", new WorldPoint(1224, 3764, 0)),
	BLACK_WARLOCK_IN_AND_AROUND_THE_FARMING_GUILD_2(BLACK_WARLOCK, "In and around the Farming Guild", "CIR", new WorldPoint(1242, 3760, 0)),
	BLACK_WARLOCK_IN_AND_AROUND_THE_FARMING_GUILD_3(BLACK_WARLOCK, "In and around the Farming Guild", "CIR", new WorldPoint(1260, 3750, 0)),
	BLACK_WARLOCK_IN_AND_AROUND_THE_FARMING_GUILD_4(BLACK_WARLOCK, "In and around the Farming Guild", "CIR", new WorldPoint(1220, 3747, 0)),
	BLACK_WARLOCK_IN_AND_AROUND_THE_FARMING_GUILD_5(BLACK_WARLOCK, "In and around the Farming Guild", "CIR", new WorldPoint(1233, 3745, 0)),

	ORANGE_SALAMANDER_NECROPOLIS_HUNTER_1(ORANGE_SALAMANDER, "Necropolis (w/ Beneath Cursed Sands)", "AKP", new WorldPoint(3285, 2741, 0)),
	ORANGE_SALAMANDER_NECROPOLIS_HUNTER_2(ORANGE_SALAMANDER, "Necropolis (w/ Beneath Cursed Sands)", "AKP", new WorldPoint(3285, 2739, 0)),
	ORANGE_SALAMANDER_NECROPOLIS_HUNTER_3(ORANGE_SALAMANDER, "Necropolis (w/ Beneath Cursed Sands)", "AKP", new WorldPoint(3287, 2739, 0)),
	ORANGE_SALAMANDER_NECROPOLIS_HUNTER_4(ORANGE_SALAMANDER, "Necropolis (w/ Beneath Cursed Sands)", "AKP", new WorldPoint(3288, 2738, 0)),
	ORANGE_SALAMANDER_NECROPOLIS_HUNTER_5(ORANGE_SALAMANDER, "Necropolis (w/ Beneath Cursed Sands)", "AKP", new WorldPoint(3286, 2738, 0)),
	ORANGE_SALAMANDER_UZER_HUNTER_AREA_1(ORANGE_SALAMANDER, "Uzer Hunter area", "DLQ", new WorldPoint(3405, 3133, 0)),
	ORANGE_SALAMANDER_UZER_HUNTER_AREA_2(ORANGE_SALAMANDER, "Uzer Hunter area", "DLQ", new WorldPoint(3403, 3090, 0)),
	ORANGE_SALAMANDER_UZER_HUNTER_AREA_3(ORANGE_SALAMANDER, "Uzer Hunter area", "DLQ", new WorldPoint(3417, 3073, 0)),

	RAZOR_BACKED_KEBBIT_PISCATORIS_HUNTER_AREA_1(RAZOR_BACKED_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2353, 3595, 0)),
	RAZOR_BACKED_KEBBIT_PISCATORIS_HUNTER_AREA_2(RAZOR_BACKED_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2360, 3611, 0)),
	RAZOR_BACKED_KEBBIT_PISCATORIS_HUNTER_AREA_3(RAZOR_BACKED_KEBBIT, "Piscatoris Hunter area", "AKQ", new WorldPoint(2357, 3624, 0)),

	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_1(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2705, 3780, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_2(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2715, 3779, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_3(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2711, 3778, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_4(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2717, 3776, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_5(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2707, 3774, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_6(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2712, 3773, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_7(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2714, 3770, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_8(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2716, 3770, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_9(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2711, 3768, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_10(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2715, 3766, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_11(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2715, 3764, 0)),
	SABRE_TOOTHED_KEBBIT_RELLEKKA_HUNTER_AREA_12(SABRE_TOOTHED_KEBBIT, "Rellekka Hunter area", "DKS", new WorldPoint(2720, 3764, 0)),

	GREY_CHINCHOMPA_PISCATORIS_HUNTER_AREA_1(GREY_CHINCHOMPA, "Piscatoris Hunter area", "AKQ", new WorldPoint(2332, 3626, 0)),
	GREY_CHINCHOMPA_PISCATORIS_HUNTER_AREA_2(GREY_CHINCHOMPA, "Piscatoris Hunter area", "AKQ", new WorldPoint(2351, 3534, 0)),
	GREY_CHINCHOMPA_PISCATORIS_HUNTER_AREA_3(GREY_CHINCHOMPA, "Piscatoris Hunter area", "AKQ", new WorldPoint(2317, 3539, 0)),
	GREY_CHINCHOMPA_PISCATORIS_HUNTER_AREA_4(GREY_CHINCHOMPA, "Piscatoris Hunter area", "AKQ", new WorldPoint(2350, 3540, 0)),
	GREY_CHINCHOMPA_PISCATORIS_HUNTER_AREA_5(GREY_CHINCHOMPA, "Piscatoris Hunter area", "AKQ", new WorldPoint(2341, 3618, 0)),
	GREY_CHINCHOMPA_PISCATORIS_HUNTER_AREA_6(GREY_CHINCHOMPA, "Piscatoris Hunter area", "AKQ", new WorldPoint(2321, 3611, 0)),
	GREY_CHINCHOMPA_PISCATORIS_HUNTER_AREA_7(GREY_CHINCHOMPA, "Piscatoris Hunter area", "AKQ", new WorldPoint(2339, 3593, 0)),
	GREY_CHINCHOMPA_KOUREND_WOODLAND(GREY_CHINCHOMPA, "Kourend Woodland", "", new WorldPoint(1480, 3503, 0)),
	GREY_CHINCHOMPA_ISLE_OF_SOULS(GREY_CHINCHOMPA, "Isle of Souls", "BJP", new WorldPoint(2126, 2949, 0)),

	SABRE_TOOTHED_KYAT_RELLEKKA_HUNTER_AREA_1(SABRE_TOOTHED_KYATT, "Rellekka Hunter area", "DKS", new WorldPoint(2708, 3796, 0)),
	SABRE_TOOTHED_KYAT_RELLEKKA_HUNTER_AREA_2(SABRE_TOOTHED_KYATT, "Rellekka Hunter area", "DKS", new WorldPoint(2725, 3791, 0)),
	SABRE_TOOTHED_KYAT_RELLEKKA_HUNTER_AREA_3(SABRE_TOOTHED_KYATT, "Rellekka Hunter area", "DKS", new WorldPoint(2696, 3790, 0)),
	SABRE_TOOTHED_KYAT_RELLEKKA_HUNTER_AREA_4(SABRE_TOOTHED_KYATT, "Rellekka Hunter area", "DKS", new WorldPoint(2702, 3790, 0)),
	SABRE_TOOTHED_KYAT_RELLEKKA_HUNTER_AREA_5(SABRE_TOOTHED_KYATT, "Rellekka Hunter area", "DKS", new WorldPoint(2741, 3790, 0)),
	SABRE_TOOTHED_KYAT_RELLEKKA_HUNTER_AREA_6(SABRE_TOOTHED_KYATT, "Rellekka Hunter area", "DKS", new WorldPoint(2734, 3780, 0)),

	DARK_KEBBIT_PISCATORIS_FALCONRY_AREA_1(DARK_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2389, 3598, 0)),
	DARK_KEBBIT_PISCATORIS_FALCONRY_AREA_2(DARK_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2368, 3594, 0)),
	DARK_KEBBIT_PISCATORIS_FALCONRY_AREA_3(DARK_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2388, 3590, 0)),
	DARK_KEBBIT_PISCATORIS_FALCONRY_AREA_4(DARK_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2368, 3589, 0)),
	DARK_KEBBIT_PISCATORIS_FALCONRY_AREA_5(DARK_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2380, 3581, 0)),
	DARK_KEBBIT_PISCATORIS_FALCONRY_AREA_6(DARK_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2365, 3579, 0)),
	DARK_KEBBIT_PISCATORIS_FALCONRY_AREA_7(DARK_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2374, 3575, 0)),

	PYRE_FOX_AVIUM_SAVANNAH_1(PYRE_FOX, "Avium Savannah", "AJP", new WorldPoint(1613, 2995, 0)),
	PYRE_FOX_AVIUM_SAVANNAH_2(PYRE_FOX, "Avium Savannah", "AJP", new WorldPoint(1617, 2997, 0)),
	PYRE_FOX_AVIUM_SAVANNAH_3(PYRE_FOX, "Avium Savannah", "AJP", new WorldPoint(1621, 2994, 0)),
	PYRE_FOX_AVIUM_SAVANNAH_4(PYRE_FOX, "Avium Savannah", "AJP", new WorldPoint(1616, 3003, 0)),
	PYRE_FOX_AVIUM_SAVANNAH_5(PYRE_FOX, "Avium Savannah", "AJP", new WorldPoint(1613, 3001, 0)),

	RED_SALAMANDER_OURANIA_HUNTER_AREA_1(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2464, 3251, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_2(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2468, 3242, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_3(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2472, 3239, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_4(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2474, 3237, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_5(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2476, 3236, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_6(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2449, 3226, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_7(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2448, 3225, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_8(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2449, 3224, 0)),
	RED_SALAMANDER_OURANIA_HUNTER_AREA_9(RED_SALAMANDER, "Ourania Hunter area", "", new WorldPoint(2453, 3221, 0)),

	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_1(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2266, 3411, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_2(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2268, 3409, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_3(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2269, 3413, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_4(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2271, 3411, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_5(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2273, 3404, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_6(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2275, 3407, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_7(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2276, 3401, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_8(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2277, 3404, 0)),
	RED_CHINCHOMPA_GWENITH_HUNTER_AREA_9(RED_CHINCHOMPA, "Gwenith Hunter area", "", new WorldPoint(2279, 3405, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_1(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2557, 2936, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_2(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2553, 2935, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_3(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2557, 2932, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_4(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2559, 2918, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_5(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2556, 2914, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_6(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2559, 2911, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_7(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2497, 2909, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_8(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2501, 2906, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_9(RED_CHINCHOMPA, "Feldip Hunter area", "AKS", new WorldPoint(2497, 2901, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_10(RED_CHINCHOMPA, "Feldip Hunter Area", "AKS", new WorldPoint(2507, 2885, 0)),
	RED_CHINCHOMPA_FELDIP_HUNTER_AREA_11(RED_CHINCHOMPA, "Feldip Hunter Area", "AKS", new WorldPoint(2503, 2881, 0)),

	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_1(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2266, 3411, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_2(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2268, 3409, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_3(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2269, 3413, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_4(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2271, 3411, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_5(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2273, 3404, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_6(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2275, 3407, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_7(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2276, 3401, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_8(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2277, 3404, 0)),
	RED_CHINCHOMPA_2_GWENITH_HUNTER_AREA_9(RED_CHINCHOMPA_2, "Gwenith Hunter area", "", new WorldPoint(2279, 3405, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_1(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2557, 2936, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_2(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2553, 2935, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_3(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2557, 2932, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_4(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2559, 2918, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_5(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2556, 2914, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_6(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2559, 2911, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_7(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2497, 2909, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_8(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2501, 2906, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_9(RED_CHINCHOMPA_2, "Feldip Hunter area", "AKS", new WorldPoint(2497, 2901, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_10(RED_CHINCHOMPA_2, "Feldip Hunter Area", "AKS", new WorldPoint(2507, 2885, 0)),
	RED_CHINCHOMPA_2_FELDIP_HUNTER_AREA_11(RED_CHINCHOMPA_2, "Feldip Hunter Area", "AKS", new WorldPoint(2503, 2881, 0)),

	MOONLIGHT_ANTELOPE_HUNTERS_GUILD_1(MOONLIGHT_ANTELOPE, "Hunters' Guild", "", new WorldPoint(1555, 9416, 0)),
	MOONLIGHT_ANTELOPE_HUNTERS_GUILD_2(MOONLIGHT_ANTELOPE, "Hunters' Guild", "", new WorldPoint(1562, 9417, 0)),
	MOONLIGHT_ANTELOPE_HUNTERS_GUILD_3(MOONLIGHT_ANTELOPE, "Hunters' Guild", "", new WorldPoint(1557, 9422, 0)),
	MOONLIGHT_ANTELOPE_HUNTERS_GUILD_4(MOONLIGHT_ANTELOPE, "Hunters' Guild", "", new WorldPoint(1561, 9421, 0)),

	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_1(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3710, 3883, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_2(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3697, 3876, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_3(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3683, 3870, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_4(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3681, 3865, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_5(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3753, 3851, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_6(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3717, 3840, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_7(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3705, 3827, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_8(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3704, 3808, 0)),
	HERBIBOAR_MUSHROOM_FOREST_FOSSIL_ISLAND_9(HERBIBOAR, "Mushroom Forest on Fossil Island", "", new WorldPoint(3696, 3796, 0)),

	TECU_SALAMANDER_RALOS_RISE_1(TECU_SALAMANDER, "Ralos' Rise", "", new WorldPoint(1471, 3087, 0)),
	TECU_SALAMANDER_RALOS_RISE_2(TECU_SALAMANDER, "Ralos' Rise", "", new WorldPoint(1472, 3096, 0)),
	TECU_SALAMANDER_RALOS_RISE_3(TECU_SALAMANDER, "Ralos' Rise", "", new WorldPoint(1473, 3086, 0)),
	TECU_SALAMANDER_RALOS_RISE_4(TECU_SALAMANDER, "Ralos' Rise", "", new WorldPoint(1475, 3101, 0)),
	TECU_SALAMANDER_RALOS_RISE_5(TECU_SALAMANDER, "Ralos' Rise", "", new WorldPoint(1477, 3099, 0)),

	MOONLIGHT_MOTH_HUNTER_GUILD_1(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1562, 9441, 0)),
	MOONLIGHT_MOTH_HUNTER_GUILD_2(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1573, 9441, 0)),
	MOONLIGHT_MOTH_HUNTER_GUILD_3(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1570, 9444, 0)),
	MOONLIGHT_MOTH_HUNTER_GUILD_4(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1573, 9446, 0)),
	MOONLIGHT_MOTH_HUNTER_GUILD_5(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1554, 9443, 0)),
	MOONLIGHT_MOTH_HUNTER_GUILD_6(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1568, 9439, 0)),
	MOONLIGHT_MOTH_HUNTER_GUILD_7(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1557, 9427, 0)),
	MOONLIGHT_MOTH_HUNTER_GUILD_8(MOONLIGHT_MOTH, "Hunters' Guild", "", new WorldPoint(1565, 9432, 0)),
	MOONLIGHT_MOTH_NEYPOTZLI_1(MOONLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1554, 9433, 0)),
	MOONLIGHT_MOTH_NEYPOTZLI_2(MOONLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1349, 9574, 0)),
	MOONLIGHT_MOTH_NEYPOTZLI_3(MOONLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1481, 9690, 0)),
	MOONLIGHT_MOTH_NEYPOTZLI_4(MOONLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1507, 9677, 0)),

	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_1(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1735, 3008, 0)),
	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_2(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1738, 3003, 0)),
	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_3(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1744, 3006, 0)),
	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_4(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1746, 3011, 0)),
	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_5(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1749, 3016, 0)),
	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_6(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1750, 3001, 0)),
	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_7(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1754, 3006, 0)),
	SUNLIGHT_ANTELOPE_AVIUM_SAVANNAH_8(SUNLIGHT_ANTELOPE, "Avium Savannah", "AJP", new WorldPoint(1755, 3013, 0)),

	DASHING_KEBBIT_PISCATORIS_FALCONRY_AREA_1(DASHING_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2390, 3588, 0)),
	DASHING_KEBBIT_PISCATORIS_FALCONRY_AREA_2(DASHING_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2375, 3578, 0)),
	DASHING_KEBBIT_PISCATORIS_FALCONRY_AREA_3(DASHING_KEBBIT, "Piscatoris falconry area", "AKQ", new WorldPoint(2367, 3576, 0)),

	SUNLIGHT_MOTH_AVIUM_SAVANNAH_1(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1550, 3016, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_2(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1551, 3013, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_3(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1553, 3016, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_4(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1555, 3012, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_5(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1556, 3016, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_6(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1558, 3015, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_7(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1559, 3015, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_8(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1735, 3012, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_9(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1565, 3016, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_10(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1568, 3011, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_11(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1568, 3020, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_12(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1571, 3016, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_13(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1573, 3024, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_14(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1575, 3021, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_15(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1576, 3017, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_16(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1576, 3023, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_17(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1577, 3024, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_18(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1578, 3021, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_19(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1582, 3024, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_20(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1582, 3031, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_21(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1583, 3011, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_22(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1584, 3021, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_23(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1585, 3009, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_24(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1585, 3026, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_25(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1586, 3013, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_26(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1586, 3020, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_27(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1588, 3024, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_28(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1588, 3027, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_29(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1589, 3010, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_30(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1590, 3036, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_31(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1592, 3012, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_32(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1596, 3020, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_33(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1598, 3016, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_34(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1598, 3029, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_35(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1551, 3089, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_36(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1551, 3094, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_37(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1553, 3091, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_38(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1555, 3085, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_39(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1554, 3095, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_40(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1556, 3091, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_41(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1557, 3085, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_42(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1556, 3088, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_43(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1557, 3094, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_44(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1560, 3086, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_45(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1561, 3092, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_46(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1560, 3094, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_47(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1562, 3088, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_48(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1562, 3093, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_49(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1564, 3087, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_50(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1568, 2995, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_51(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1570, 2987, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_52(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1570, 2998, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_53(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1572, 2995, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_54(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1572, 3003, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_55(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1575, 3000, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_56(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1578, 2986, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_57(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1579, 2991, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_58(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1580, 2995, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_59(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1582, 3002, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_60(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1584, 2991, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_61(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1584, 2999, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_62(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1588, 2994, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_63(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1588, 3008, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_64(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1590, 2998, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_65(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1591, 2988, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_66(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1592, 2983, 0)),
	SUNLIGHT_MOTH_AVIUM_SAVANNAH_67(SUNLIGHT_MOTH, "Avium Savannah", "AJP", new WorldPoint(1596, 2985, 0)),

	SUNLIGHT_MOTH_AVIUM_NEYPOTZLI_1(SUNLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1434, 9632, 0)),
	SUNLIGHT_MOTH_AVIUM_NEYPOTZLI_2(SUNLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1437, 9636, 0)),
	SUNLIGHT_MOTH_AVIUM_NEYPOTZLI_3(SUNLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1441, 9626, 0)),
	SUNLIGHT_MOTH_AVIUM_NEYPOTZLI_4(SUNLIGHT_MOTH, "Neypotzli", "", new WorldPoint(1444, 9633, 0));

	@Getter
	private final net.runelite.client.plugins.hunterrumours.enums.Rumour Rumour;

	@Getter
	private final String LocationName;

	@Getter
	private final String FairyRingCode;

	@Getter
	private final WorldPoint WorldPoint;

	/**
	 * Gets all RumourLocations linked to the given Rumour
	 */
	public static Set<RumourLocation> getLocationsForRumour(Rumour rumour)
	{
		return getLocationsStreamForRumour(rumour).collect(Collectors.toSet());
	}

	/**
	 * Gets a Set of RumourLocations linked to the given Rumour -- grouped by LocationName, then collapsed to a single
	 * entry per unique LocationName.
	 * @param rumour
	 * @return
	 */
	public static Set<RumourLocation> getCollapsedLocationsForRumour(Rumour rumour) {
		var groupedLocations = getGroupedLocationsForRumour(rumour);
		return groupedLocations.map(l -> l.getValue().get(0)).collect(Collectors.toSet());
	}

	/**
	 * Gets all RumourLocations linked to the given Rumour, grouped by LocationName
	 */
	public static Stream<Map.Entry<String, List<RumourLocation>>> getGroupedLocationsForRumour(Rumour rumour)
	{
		var locations = getLocationsStreamForRumour(rumour).toArray(RumourLocation[]::new);

		// Create a map to remember the ordering of declaration of the locations (by the first instance of
		// each location name). This is so we can group by name (which loses this ordering) and then sort the groups
		// by the original declaration order.
		Map<String, Integer> locationDeclarationOrder = new HashMap<>();
		int i = 0;
		for(RumourLocation location : locations) {
			if(!locationDeclarationOrder.containsKey(location.getLocationName())) {
				locationDeclarationOrder.put(location.getLocationName(), i++);
			}
		}

		// I do not know Java very well and there is surely a better way to do this, but... I think it works, so, oh well.
		// Group the locations by their name, then return those groups sorted by declaration order of their first location.
		return Arrays.stream(locations)
				.collect(Collectors.groupingBy(RumourLocation::getLocationName))
				.entrySet()
				.stream()
				.sorted(Comparator.comparingInt(a -> locationDeclarationOrder.get(a.getKey())));
	}

	private static Stream<RumourLocation> getLocationsStreamForRumour(Rumour rumour)
	{
		return Arrays.stream(RumourLocation.values()).filter(loc -> loc.getRumour() == rumour);
	}
}