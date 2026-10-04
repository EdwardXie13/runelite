package net.runelite.client.plugins.autoBossCore.combat;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Menu-action helpers that fire actions straight at the client, bypassing the Oculus
 * Orb's gating. All methods marshal onto the client thread.
 * <p>
 * Modeled on the pattern VorkathAuto uses: pack the right args into
 * {@link Client#menuAction} and let the engine's packet queue handle it.
 */
@Slf4j
@Singleton
public class ClickRouter
{
	/** Packed widget id of the inventory container (ComponentID.INVENTORY_CONTAINER). */
	public static final int INVENTORY_WIDGET = 9764864;

	/** The item target string color used by vanilla menus for inventory items. */
	public static final String ITEM_COLOR_PREFIX = "<col=ff9040>";
	public static final String ITEM_COLOR_SUFFIX = "</col>";

	private final Client client;
	private final ClientThread clientThread;

	@Inject
	public ClickRouter(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	// -----------------------------------------------------------------
	// Walking.
	// -----------------------------------------------------------------

	public void walkTo(WorldPoint tile)
	{
		if (tile == null) return;
		clientThread.invoke(() -> {
			LocalPoint lp = LocalPoint.fromWorld(client, tile);
			if (lp == null)
			{
				log.debug("[router] walkTo rejected -- no LocalPoint for {}", tile);
				return;
			}
			int plane = client.getTopLevelWorldView().getPlane();
			Point canvas = Perspective.localToCanvas(client, lp, plane);
			if (canvas == null)
			{
				log.debug("[router] walkTo rejected -- tile not on canvas: {}", tile);
				return;
			}
			// client.menuAction(..., MenuAction.WALK) is unreliable because the server
			// doesn't accept scene OR canvas params for WALK consistently. Instead we
			// dispatch real AWT MouseEvents to the game's canvas -- processed exactly
			// like a human left-click. Pattern lifted from VorkathAuto.dispatchClick.
			dispatchCanvasClick(canvas.getX(), canvas.getY());
		});
	}

	private void dispatchCanvasClick(int canvasX, int canvasY)
	{
		Canvas c = client.getCanvas();
		if (c == null) return;
		long t = System.currentTimeMillis();
		MouseMotionListener[] motion = c.getMouseMotionListeners();
		MouseListener[] mouse = c.getMouseListeners();

		MouseEvent moved = new MouseEvent(c, MouseEvent.MOUSE_MOVED, t, 0, canvasX, canvasY, 0, false);
		for (MouseMotionListener l : motion)
		{
			try { l.mouseMoved(moved); } catch (Throwable ex) { log.debug("[router] mouseMoved threw", ex); }
		}

		MouseEvent pressed = new MouseEvent(c, MouseEvent.MOUSE_PRESSED, t + 1,
			InputEvent.BUTTON1_DOWN_MASK, canvasX, canvasY, 1, false, MouseEvent.BUTTON1);
		for (MouseListener l : mouse)
		{
			try { l.mousePressed(pressed); } catch (Throwable ex) { log.debug("[router] mousePressed threw", ex); }
		}

		MouseEvent released = new MouseEvent(c, MouseEvent.MOUSE_RELEASED, t + 2, 0,
			canvasX, canvasY, 1, false, MouseEvent.BUTTON1);
		for (MouseListener l : mouse)
		{
			try { l.mouseReleased(released); } catch (Throwable ex) { log.debug("[router] mouseReleased threw", ex); }
		}

		MouseEvent clicked = new MouseEvent(c, MouseEvent.MOUSE_CLICKED, t + 3, 0,
			canvasX, canvasY, 1, false, MouseEvent.BUTTON1);
		for (MouseListener l : mouse)
		{
			try { l.mouseClicked(clicked); } catch (Throwable ex) { log.debug("[router] mouseClicked threw", ex); }
		}
	}

	// -----------------------------------------------------------------
	// NPCs.
	// -----------------------------------------------------------------

	public void attackNpc(NPC npc)
	{
		if (npc == null) return;
		clientThread.invoke(() -> client.menuAction(
			0, 0,
			MenuAction.NPC_SECOND_OPTION,
			npc.getIndex(),
			-1,
			"Attack",
			buildNpcTarget(npc)
		));
	}

	public void interactNpc(NPC npc, MenuAction action, String option)
	{
		if (npc == null) return;
		clientThread.invoke(() -> client.menuAction(
			0, 0,
			action,
			npc.getIndex(),
			-1,
			option,
			buildNpcTarget(npc)
		));
	}

	// -----------------------------------------------------------------
	// GameObjects.
	// -----------------------------------------------------------------

	public void interactGameObject(GameObject go, MenuAction action, String option)
	{
		if (go == null) return;
		clientThread.invoke(() -> {
			net.runelite.api.Point sceneMin = go.getSceneMinLocation();
			if (sceneMin == null) return;
			String name = objectName(go);
			client.menuAction(
				sceneMin.getX(), sceneMin.getY(),
				action,
				go.getId(),
				-1,
				option,
				name
			);
		});
	}

	// -----------------------------------------------------------------
	// Ground items.
	// -----------------------------------------------------------------

	public void takeGroundItem(int itemId, WorldPoint tile, String itemName)
	{
		if (tile == null) return;
		clientThread.invoke(() -> {
			LocalPoint lp = LocalPoint.fromWorld(client, tile);
			if (lp == null) return;
			// Ground-item menuAction uses SCENE coords (unlike WALK which uses canvas).
			client.menuAction(
				lp.getSceneX(), lp.getSceneY(),
				MenuAction.GROUND_ITEM_THIRD_OPTION,
				itemId,
				-1,
				"Take",
				itemName == null ? "" : itemName
			);
		});
	}

	// -----------------------------------------------------------------
	// Inventory items (eat / drink / etc).
	// -----------------------------------------------------------------

	/**
	 * Fire the "Eat"/"Drink"/etc action on the first inventory slot holding this item id.
	 * Returns false if the item is not in the inventory.
	 */
	public boolean useInventoryItem(int itemId, String option)
	{
		int slot = findInventorySlot(itemId);
		if (slot < 0) return false;
		clientThread.invoke(() -> {
			ItemComposition comp = client.getItemDefinition(itemId);
			String name = comp == null ? "" : comp.getName();
			client.menuAction(
				slot,
				INVENTORY_WIDGET,
				MenuAction.CC_OP,
				2,                       // identifier 2 = Eat/Drink/second-op on most consumables
				itemId,
				option,
				ITEM_COLOR_PREFIX + name + ITEM_COLOR_SUFFIX
			);
		});
		return true;
	}

	/** Low-level inventory CC_OP for callers that need a non-default identifier. */
	public boolean useInventoryItem(int itemId, int identifier, String option)
	{
		int slot = findInventorySlot(itemId);
		if (slot < 0) return false;
		clientThread.invoke(() -> {
			ItemComposition comp = client.getItemDefinition(itemId);
			String name = comp == null ? "" : comp.getName();
			client.menuAction(
				slot,
				INVENTORY_WIDGET,
				MenuAction.CC_OP,
				identifier,
				itemId,
				option,
				ITEM_COLOR_PREFIX + name + ITEM_COLOR_SUFFIX
			);
		});
		return true;
	}

	// -----------------------------------------------------------------
	// Vardorvis QTE / "captcha" widgets.
	// -----------------------------------------------------------------

	/**
	 * Click a single QTE_MODEL child in the Vardorvis Quick-Time-Event interface.
	 * Replays the exact menuAction a human click produces:
	 * {@code CC_OP id=1 itemId=-1 param0=-1 param1=widgetId option='Destroy' target=''}.
	 */
	public void destroyQte(int widgetId)
	{
		clientThread.invoke(() -> client.menuAction(
			-1, widgetId,
			MenuAction.CC_OP,
			1, -1,
			"Destroy", ""
		));
	}

	// -----------------------------------------------------------------
	// Prayer widgets.
	// -----------------------------------------------------------------

	public void activatePrayer(int widgetId, String name)
	{
		clientThread.invoke(() -> client.menuAction(
			-1, widgetId,
			MenuAction.CC_OP,
			1, -1,
			"Activate",
			name == null ? "" : name
		));
	}

	public void deactivatePrayer(int widgetId, String name)
	{
		clientThread.invoke(() -> client.menuAction(
			-1, widgetId,
			MenuAction.CC_OP,
			1, -1,
			"Deactivate",
			name == null ? "" : name
		));
	}

	// -----------------------------------------------------------------
	// Helpers.
	// -----------------------------------------------------------------

	private int findInventorySlot(int itemId)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		if (inv == null) return -1;
		Item[] items = inv.getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] != null && items[i].getId() == itemId) return i;
		}
		return -1;
	}

	private String buildNpcTarget(NPC npc)
	{
		String name = npc.getName() == null ? "" : npc.getName();
		int level = npc.getCombatLevel();
		return "<col=ffff00>" + name + "<col=ff0000>" + (level > 0 ? "  (level-" + level + ")" : "");
	}

	private String objectName(GameObject go)
	{
		try
		{
			net.runelite.api.ObjectComposition c = client.getObjectDefinition(go.getId());
			if (c != null)
			{
				if (c.getImpostor() != null) c = c.getImpostor();
				return c.getName() == null ? "" : c.getName();
			}
		}
		catch (Throwable ignore) { }
		return "";
	}
}
