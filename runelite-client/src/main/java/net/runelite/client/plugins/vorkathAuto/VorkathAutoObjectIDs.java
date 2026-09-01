package net.runelite.client.plugins.vorkathAuto;

import lombok.experimental.UtilityClass;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;

@UtilityClass
public class VorkathAutoObjectIDs {
    public final int FAIRY_RING_29495_POH = 29228;
    public final int MYSTERIOUS_RUINS = 25380;

    public final int POOL_OF_REVITALISATION = ObjectID.POOL_OF_REVITALISATION;
    public final int POOL_OF_REJUVENATION = ObjectID.POOL_OF_REJUVENATION;
    public final int FANCY_POOL_OF_REJUVENATION = ObjectID.FANCY_POOL_OF_REJUVENATION;
    public final int ORNATE_POOL_OF_REJUVENATION = ObjectID.ORNATE_POOL_OF_REJUVENATION;

    public final int PORTAL_NEXUS = ObjectID.PORTAL_NEXUS;
    public final int LUNAR_ISLE_BANK_BOOTH = ObjectID.BANK_BOOTH_16700;
    public final int VORKATH_ICE_CHUNKS_OUTSIDE = ObjectID.ICE_CHUNKS_31990;
    public final int VORKATH_ICE_CHUNKS_INSIDE = ObjectID.ICE_CHUNKS_47324;

    public static GameObject portalNexus = null;
    public static GameObject restorationPoolPOH = null;
    public static GameObject lunarIsleBankBooth = null;
    public static GameObject vorkathIceChunksOutside = null;
    public static GameObject vorkathIceChunksInside = null;

    public static void assignObjects(Client client, GameObjectSpawned event) {
        GameObject obj = event.getGameObject();
        int id = obj.getId();

        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
        if (objWp == null)
            return;

        switch (id) {
            case PORTAL_NEXUS:
                portalNexus = obj;
                break;

            case POOL_OF_REVITALISATION:
            case POOL_OF_REJUVENATION:
            case FANCY_POOL_OF_REJUVENATION:
            case ORNATE_POOL_OF_REJUVENATION:
                VorkathAutoWorldPoints.INFRONT_OF_POOL = new WorldPoint(objWp.getX()-2, objWp.getY(), 0);
                restorationPoolPOH = obj;
                break;
            case LUNAR_ISLE_BANK_BOOTH:
                if (objWp.equals(VorkathAutoWorldPoints.LUNAR_ISLE_BANK_BOOTH_TILE)) {
                    lunarIsleBankBooth = obj;
                }
                break;
            case VORKATH_ICE_CHUNKS_OUTSIDE:
                vorkathIceChunksOutside = obj;
                break;
            case VORKATH_ICE_CHUNKS_INSIDE:
                vorkathIceChunksInside = obj;
                break;
        }
    }

    public static void assignObjects(Client client, GameObjectDespawned event) {
        GameObject obj = event.getGameObject();
        int id = obj.getId();

        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
        if (objWp == null)
            return;

        switch (id) {
            case PORTAL_NEXUS:
                portalNexus = null;
                break;

            case POOL_OF_REVITALISATION:
            case POOL_OF_REJUVENATION:
            case FANCY_POOL_OF_REJUVENATION:
            case ORNATE_POOL_OF_REJUVENATION:
                restorationPoolPOH = null;
                break;

            case LUNAR_ISLE_BANK_BOOTH:
                if (objWp.equals(VorkathAutoWorldPoints.LUNAR_ISLE_BANK_BOOTH_TILE)) {
                    lunarIsleBankBooth = null;
                }
                break;
            case VORKATH_ICE_CHUNKS_OUTSIDE:
                vorkathIceChunksOutside = null;
                break;
            case VORKATH_ICE_CHUNKS_INSIDE:
                vorkathIceChunksInside = null;
                break;
        }
    }

    public void setAllVarsNull() {
        portalNexus = null;
        restorationPoolPOH = null;
        lunarIsleBankBooth = null;
    }

    public static void assignObjects(Client client, NpcSpawned event) {

    }
}
