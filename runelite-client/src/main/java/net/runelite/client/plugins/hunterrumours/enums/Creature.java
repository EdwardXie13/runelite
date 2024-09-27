package net.runelite.client.plugins.hunterrumours.enums;

import java.util.stream.IntStream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.NpcID;

import static net.runelite.client.plugins.hunterrumours.enums.Trap.*;

@AllArgsConstructor
public enum Creature {
    NONE(0, ItemID.COINS_995, BOX_TRAP, 0, new int[]{}),

    TROPICAL_WAGTAIL(NpcID.TROPICAL_WAGTAIL, ItemID.TROPICAL_WAGTAIL, SNARE, 19, new int[]{95, 96}),
    WILD_KEBBIT(NpcID.WILD_KEBBIT, ItemID.KEBBIT_9962, DEADFALL, 23, new int[]{128}),
    SAPPHIRE_GLACIALIS(NpcID.SAPPHIRE_GLACIALIS, ItemID.BUTTERFLY_9971, BUTTERFLY, 25, new int[]{34}),

    SWAMP_LIZARD(NpcID.SWAMP_LIZARD, ItemID.SWAMP_LIZARD, NET_TRAP, 29, new int[]{152}),
    SPINED_LARUPIA(NpcID.SPINED_LARUPIA, ItemID.LARUPIA_HAT, PIT, 31, new int[]{180}),
    BARB_TAILED_KEBBIT(NpcID.BARBTAILED_KEBBIT, ItemID.KEBBIT_9958, DEADFALL, 33, new int[]{168}),
    SNOWY_KNIGHT(NpcID.SNOWY_KNIGHT, ItemID.BUTTERFLY_9972, BUTTERFLY, 35, new int[]{44}),
    PRICKLY_KEBBIT(NpcID.PRICKLY_KEBBIT, ItemID.KEBBIT_9957, DEADFALL, 37, new int[]{204}),
    EMBERTAILED_JERBOA(NpcID.EMBERTAILED_JERBOA, ItemID.EMBERTAILED_JERBOA, BOX_TRAP, 39, new int[]{137}),
    HORNED_GRAAHK(NpcID.HORNED_GRAAHK, ItemID.GRAAHK_HEADDRESS, PIT, 41, new int[]{240}),
    SPOTTED_KEBBIT(NpcID.SPOTTED_KEBBIT, ItemID.KEBBIT_9960, FALCONRY, 43, new int[]{104}),
    BLACK_WARLOCK(NpcID.BLACK_WARLOCK, ItemID.BUTTERFLY_9973, BUTTERFLY, 45, new int[]{54}),

    ORANGE_SALAMANDER(NpcID.ORANGE_SALAMANDER, ItemID.ORANGE_SALAMANDER, NET_TRAP, 47, new int[]{224}),
    RAZOR_BACKED_KEBBIT(0, ItemID.KEBBIT_9961, NOOSE, 49, new int[]{348}), //TODO
    SABRE_TOOTHED_KEBBIT(NpcID.SABRETOOTHED_KEBBIT, ItemID.KEBBIT_9959, DEADFALL, 51, new int[]{200}),
    GREY_CHINCHOMPA(NpcID.CHINCHOMPA, ItemID.CHINCHOMPA, BOX_TRAP, 53, new int[]{198, 199}),
    SABRE_TOOTHED_KYATT(NpcID.SABRETOOTHED_KYATT, ItemID.KYATT_HAT, PIT, 53, new int[]{300}),
    DARK_KEBBIT(NpcID.DARK_KEBBIT, ItemID.KEBBIT_9963, FALCONRY, 57, new int[]{132}),
    PYRE_FOX(NpcID.PYRE_FOX, ItemID.PYRE_FOX, DEADFALL, 57, new int[]{222}),
    RED_SALAMANDER(NpcID.RED_SALAMANDER, ItemID.RED_SALAMANDER, NET_TRAP, 59, new int[]{272}),
    RED_CHINCHOMPA(NpcID.CARNIVOROUS_CHINCHOMPA, ItemID.RED_CHINCHOMPA, BOX_TRAP, 63, new int[]{265}),
    SUNLIGHT_MOTH(NpcID.SUNLIGHT_MOTH, ItemID.SUNLIGHT_MOTH, BUTTERFLY, 65, new int[]{74}),
    DASHING_KEBBIT(NpcID.DASHING_KEBBIT, ItemID.KEBBIT_9964, FALCONRY, 69,  new int[]{156}),
    SUNLIGHT_ANTELOPE(NpcID.SUNLIGHT_ANTELOPE, ItemID.SUNLIGHT_ANTELOPE, PIT, 72,  new int[]{380}),
    MOONLIGHT_MOTH(NpcID.MOONLIGHT_MOTH, ItemID.MOONLIGHT_MOTH, BUTTERFLY, 75,  new int[]{84}),
    TECU_SALAMANDER(NpcID.TECU_SALAMANDER, ItemID.TECU_SALAMANDER, NET_TRAP, 79, new int[]{344}),
    HERBIBOAR(NpcID.HERBIBOAR, ItemID.HERBIBOAR, NOOSE_HERBIBOAR, 80, IntStream.rangeClosed(1950, 2461).toArray()),
    MOONLIGHT_ANTELOPE(NpcID.MOONLIGHT_ANTELOPE, ItemID.MOONLIGHT_ANTELOPE, PIT, 91, new int[]{450});

    @Getter
    private final int NpcId;

    @Getter
    private final int ItemId;

    @Getter
    private final net.runelite.client.plugins.hunterrumours.enums.Trap Trap;

    @Getter
    private final int HunterLevel;

    @Getter
    private final int[] PossibleXpDrops;
}
