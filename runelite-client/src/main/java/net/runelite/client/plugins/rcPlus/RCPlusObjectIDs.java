package net.runelite.client.plugins.rcPlus;

import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;

public class RCPlusObjectIDs {
    //=== AIR ALTAR ===
    public static final int PORTAL_34748 = ObjectID.PORTAL_34748;
    public static final int AIR_MYSTERIOUS_RUINS = 34813;
    public static final int AIR_ALTAR_34760 = ObjectID.ALTAR_34760;

    public static final int BANK_BOOTH_24101 = ObjectID.BANK_BOOTH_24101;

    public static GameObject airAltarPortal = null;
    public static GameObject airMysteriousRuins = null;
    public static GameObject airAltarAltar = null;

    public static GameObject airFaladorBankBooth2 = null;
    public static GameObject airFaladorBankBooth3 = null;
    public static GameObject airFaladorBankBooth4 = null;

    //=== COSMIC ALTAR ===
    public static final int JUTTING_WALL_17002 = ObjectID.JUTTING_WALL_17002;
    public static final int BANK_CHEST_26711 = ObjectID.BANK_CHEST_26711;
    public static final int COSMIC_MYSTERIOUS_RUINS = 34819;
    public static final int COSMIC_ALTAR_34766 = ObjectID.ALTAR_34766;
    public static final int COSMIC_PORTAL = ObjectID.PORTAL_34754;

    public static GameObject juttingWall = null;
    public static GameObject zanarisBankChest3 = null;
    public static GameObject zanarisBankChest4 = null;
    public static GameObject zanarisBankChest5 = null;
    public static GameObject cosmicAltarAltar = null;
    public static GameObject cosmicAltarNorthPortal = null;
    public static GameObject cosmicAltarEastPortal = null;
    public static GameObject cosmicAltarSouthPortal = null;
    public static GameObject cosmicAltarWestPortal = null;

    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case PORTAL_34748:
                airAltarPortal = obj;
                break;
            case AIR_MYSTERIOUS_RUINS:
                System.out.println("air ruins spawned");
                airMysteriousRuins = obj;
                break;
            case AIR_ALTAR_34760:
                airAltarAltar = obj;
                break;
            case BANK_BOOTH_24101:
                WorldPoint bankBoothWp = event.getTile().getWorldLocation();
                if(bankBoothWp.equals(new WorldPoint(3011, 3354, 0))) {
                    airFaladorBankBooth2 = obj;
                } else if(bankBoothWp.equals(new WorldPoint(3012, 3354, 0))) {
                    airFaladorBankBooth3 = obj;
                } else if(bankBoothWp.equals(new WorldPoint(3013, 3354, 0))) {
                    airFaladorBankBooth4 = obj;
                }
                break;
        }
    }

    public static void assignObjects(GameObjectDespawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case PORTAL_34748:
                airAltarPortal = null;
                break;
            case AIR_MYSTERIOUS_RUINS:
                airMysteriousRuins = null;
                break;
            case AIR_ALTAR_34760:
                airAltarAltar = null;
                break;
            case BANK_BOOTH_24101:
                airFaladorBankBooth2 = null;
                airFaladorBankBooth3 = null;
                airFaladorBankBooth4 = null;
                break;
        }
    }
}
