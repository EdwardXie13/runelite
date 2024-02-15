package net.runelite.client.plugins.gotrPlus;

import lombok.experimental.UtilityClass;
import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.game.ItemManager;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@UtilityClass
public class GOTRPlusObjectIDs {
    private BufferedImage airRuneIcon = null;
    private BufferedImage waterRuneIcon = null;
    private BufferedImage earthRuneIcon = null;
    private BufferedImage fireRuneIcon = null;
    private BufferedImage mindRuneIcon = null;
    private BufferedImage chaosRuneIcon = null;
    private BufferedImage deathRuneIcon = null;
    private BufferedImage bloodRuneIcon = null;
    private BufferedImage bodyRuneIcon = null;
    private BufferedImage cosmicRuneIcon = null;
    private BufferedImage natureRuneIcon = null;
    private BufferedImage lawRuneIcon = null;

    public static GameObject guardianOfAir = null;
    public static GameObject guardianOfWater = null;
    public static GameObject guardianOfEarth = null;
    public static GameObject guardianOfFire = null;
    public static GameObject guardianOfMind = null;
    public static GameObject guardianOfChaos = null;
    public static GameObject guardianOfDeath = null;
    public static GameObject guardianOfBlood = null;
    public static GameObject guardianOfBody = null;
    public static GameObject guardianOfCosmic = null;
    public static GameObject guardianOfNature = null;
    public static GameObject guardianOfLaw = null;
    public static GameObject portalToRocks = null;

    public static void initIcons(ItemManager itemManager) {
        airRuneIcon = itemManager.getImage(ItemID.AIR_RUNE);
        waterRuneIcon = itemManager.getImage(ItemID.WATER_RUNE);
        earthRuneIcon = itemManager.getImage(ItemID.EARTH_RUNE);
        fireRuneIcon = itemManager.getImage(ItemID.FIRE_RUNE);
        mindRuneIcon = itemManager.getImage(ItemID.MIND_RUNE);
        chaosRuneIcon = itemManager.getImage(ItemID.CHAOS_RUNE);
        deathRuneIcon = itemManager.getImage(ItemID.DEATH_RUNE);
        bloodRuneIcon = itemManager.getImage(ItemID.BLOOD_RUNE);
        bodyRuneIcon = itemManager.getImage(ItemID.BODY_RUNE);
        cosmicRuneIcon = itemManager.getImage(ItemID.COSMIC_RUNE);
        natureRuneIcon = itemManager.getImage(ItemID.NATURE_RUNE);
        lawRuneIcon = itemManager.getImage(ItemID.LAW_RUNE);
    }

    public static Set<Pair<GameObject, BufferedImage>> getAltarWithIcon() {
        Set<Pair<GameObject, BufferedImage>> temp = new HashSet<>();

        Set<Pair<GameObject, BufferedImage>> guardiansAndIcons = new HashSet<>();
        guardiansAndIcons.add(Pair.of(guardianOfAir, airRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfWater, waterRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfEarth, earthRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfFire, fireRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfMind, mindRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfChaos, chaosRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfDeath, deathRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfBlood, bloodRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfBody, bodyRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfCosmic, cosmicRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfNature, natureRuneIcon));
        guardiansAndIcons.add(Pair.of(guardianOfLaw, lawRuneIcon));

        guardiansAndIcons.forEach(pair ->
                Optional.ofNullable(pair.getKey()).ifPresent(guardian -> temp.add(Pair.of(guardian, pair.getValue())))
        );

        return temp;
    }

    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case ObjectID.GUARDIAN_OF_AIR:
                guardianOfAir = obj;
                break;
            case ObjectID.GUARDIAN_OF_WATER:
                guardianOfWater = obj;
                break;
            case ObjectID.GUARDIAN_OF_EARTH:
                guardianOfEarth = obj;
                break;
            case ObjectID.GUARDIAN_OF_FIRE:
                guardianOfFire = obj;
                break;
            case ObjectID.GUARDIAN_OF_MIND:
                guardianOfMind = obj;
                break;
            case ObjectID.GUARDIAN_OF_CHAOS:
                guardianOfChaos = obj;
                break;
            case ObjectID.GUARDIAN_OF_DEATH:
                guardianOfDeath = obj;
                break;
            case ObjectID.GUARDIAN_OF_BLOOD:
                guardianOfBlood = obj;
                break;
            case ObjectID.GUARDIAN_OF_BODY:
                guardianOfBody = obj;
                break;
            case ObjectID.GUARDIAN_OF_COSMIC:
                guardianOfCosmic = obj;
                break;
            case ObjectID.GUARDIAN_OF_NATURE:
                guardianOfNature = obj;
                break;
            case ObjectID.GUARDIAN_OF_LAW:
                guardianOfLaw = obj;
                break;
            case ObjectID.PORTAL_43729:
                portalToRocks = obj;
                break;
        }
    }

    public static void assignObjects(GameObjectDespawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case ObjectID.GUARDIAN_OF_AIR:
                guardianOfAir = null;
                break;
            case ObjectID.GUARDIAN_OF_WATER:
                guardianOfWater = null;
                break;
            case ObjectID.GUARDIAN_OF_EARTH:
                guardianOfEarth = null;
                break;
            case ObjectID.GUARDIAN_OF_FIRE:
                guardianOfFire = null;
                break;
            case ObjectID.GUARDIAN_OF_MIND:
                guardianOfMind = null;
                break;
            case ObjectID.GUARDIAN_OF_CHAOS:
                guardianOfChaos = null;
                break;
            case ObjectID.GUARDIAN_OF_DEATH:
                guardianOfDeath = null;
                break;
            case ObjectID.GUARDIAN_OF_BLOOD:
                guardianOfBlood = null;
                break;
            case ObjectID.GUARDIAN_OF_BODY:
                guardianOfBody = null;
                break;
            case ObjectID.GUARDIAN_OF_COSMIC:
                guardianOfCosmic = null;
                break;
            case ObjectID.GUARDIAN_OF_NATURE:
                guardianOfNature = null;
                break;
            case ObjectID.GUARDIAN_OF_LAW:
                guardianOfLaw = null;
                break;
            case ObjectID.PORTAL_43729:
                portalToRocks = null;
                break;
        }
    }
}
