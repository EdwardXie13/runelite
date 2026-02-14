package net.runelite.client.plugins.menuentryswapextended;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Optional;

public class CustomSwapUtils {
    String POH_CONCAPET = "Tele to POH, Construct. cape*";
    public boolean isAtTile(Client client, int x, int y, int z) {
        WorldPoint localWP =
                Optional.ofNullable(client)
                .map(Client::getLocalPlayer)
                .map(Actor::getWorldLocation)
                .orElse(null);
        if (localWP != null) {
            boolean playerX = localWP.getX() == x;
            boolean playerY = localWP.getY() == y;
            boolean playerZ = localWP.getPlane() == z;
            return playerX && playerY && playerZ;
        }
        return false;
    }

    public boolean isSandFilling(List<Item> inventoryItems) {
        int conCape = ItemID.CONSTRUCT_CAPET;
        int sandbucket = ItemID.BUCKET_OF_SAND;
        return countItem(inventoryItems, conCape, 1) &&
                countItem(inventoryItems, sandbucket, 27);
    }

    private boolean countItem(List<Item> inventoryItems, int item, int count) {
        return inventoryItems.stream()
                .filter(items -> items.getId() == item)
                .count() >= count;
    }
}
