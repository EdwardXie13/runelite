package net.runelite.client.plugins.rcPlus;

import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;

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
    public static final int BANK_CHEST_26711 = ObjectID.BANK_CHEST_26711;
    public static final int FREEFORALL_PORTAL = ObjectID.FREEFORALL_PORTAL;
    public static final int FIRE_MYSTERIOUS_RUINS = 34817;
    public static final int FIRE_ALTAR_34764 = ObjectID.ALTAR_34764;
    public static GameObject feroxEnclaveBank = null;
    public static GameObject freeForAllPortal = null;
    public static GameObject fireMysteriousRuins = null;
    public static GameObject fireAltarAltar = null;

    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case PORTAL_34748:
                airAltarPortal = obj;
                break;
            case AIR_MYSTERIOUS_RUINS:
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

            //=== FIRE ALTAR ===
            case BANK_CHEST_26711:
                feroxEnclaveBank = obj;
                break;
            case FREEFORALL_PORTAL:
                freeForAllPortal = obj;
                break;
            case FIRE_MYSTERIOUS_RUINS:
                fireMysteriousRuins = obj;
                break;
            case FIRE_ALTAR_34764:
                fireAltarAltar = obj;
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

            //=== FIRE ALTAR ===
            case BANK_CHEST_26711:
                feroxEnclaveBank = null;
                break;
            case FREEFORALL_PORTAL:
                freeForAllPortal = null;
                break;
            case FIRE_MYSTERIOUS_RUINS:
                fireMysteriousRuins = null;
                break;
            case FIRE_ALTAR_34764:
                fireAltarAltar = null;
                break;
        }
    }
}
