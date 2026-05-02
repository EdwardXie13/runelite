package net.runelite.client.plugins.menuentryswapextended;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.util.List;
import java.util.Optional;

public class CustomSwapUtils {
    String POH_CONCAPET = "Tele to POH, Construct. cape*";
    String COALBAG_EMPTY = "Empty, Coal bag";
    String STEEL_PLATESKIRT_WITHDRAW1 = "Withdraw-1, Steel plateskirt";
    String STEEL_PLATESKIRT_WITHDRAW5 = "Withdraw-5, Steel plateskirt";
    String STEEL_PLATESKIRT_EXAMINE = "Examine, Steel plateskirt";
    String MITH_PLATESKIRT_WITHDRAW1 = "Withdraw-1, Mithril plateskirt";
    String MITH_PLATESKIRT_WITHDRAW5 = "Withdraw-5, Mithril plateskirt";
    String MITH_PLATESKIRT_EXAMINE = "Examine, Mithril plateskirt";
    String KOVAC_HAND_IN = "Hand-in, Kovac";
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

    public int getCountItem(List<Item> inventoryItems, int item) {
        return (int) inventoryItems.stream()
                .filter(items -> items.getId() == item)
                .count();
    }

    public boolean countItem(List<Item> inventoryItems, int item, int count) {
        return getCountItem(inventoryItems, item) >= count;
    }

    public int getRegionID(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    public boolean isBankOpen(Client client) {
        Widget bank = client.getWidget(WidgetInfo.BANK_CONTAINER);
        return bank != null && !bank.isHidden();
    }
}
