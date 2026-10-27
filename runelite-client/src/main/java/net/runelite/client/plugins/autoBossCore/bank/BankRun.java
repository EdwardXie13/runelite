package net.runelite.client.plugins.autoBossCore.bank;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.plugins.autoBossCore.state.SceneScanner;
import net.runelite.client.plugins.autoBossCore.state.WidgetReader;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared, TICK-DRIVEN banking run for bosses that can't bank in place. Scope: get from "need to bank" to a
 * standing-open bank, restoring at the POH pool on the way. It does NOT withdraw/deposit -- that loadout is
 * boss-specific, so the boss takes over once {@link #bankReady()} is true.
 *
 * <p>Pipeline (each phase advances on a real game signal, never a blocking wait):
 * <ol>
 *   <li>TELE_TO_POH   -- teletab / house-tele spell; done when the POH map region is loaded.</li>
 *   <li>RESTORE_POOL  -- Drink the rejuvenation pool once (latched on the drink anim); skipped if disabled.</li>
 *   <li>NEXUS_TO_LUNAR-- take the Portal Nexus option to the bank; done when the bank's map region loads.</li>
 *   <li>OPEN_BANK     -- click the bank booth; done when the bank interface opens.</li>
 *   <li>READY         -- bank open; {@link #step} yields the tick so the boss can withdraw/deposit.</li>
 * </ol>
 *
 * <p>Pattern mirrors the thrall manager: autoBossCore owns the mechanics; the boss decides WHEN to bank
 * ({@link #begin()}), drives it each tick ({@link #step(int)}), and does its own banking once ready.
 * POH objects are found by scanning the scene for their ids, so it works for any house layout.
 */
public final class BankRun
{
	public enum Phase { IDLE, TELE_TO_POH, RESTORE_POOL, NEXUS_TO_LUNAR, OPEN_BANK, READY }

	// Map regions.
	private static final int POH_REGION            = 7534;
	private static final int POH_TRANSITION_REGION = 11826;   // the "entering your house" loading screen
	private static final int LUNAR_ISLE_REGION     = 8253;

	// Rejuvenation-pool drink animation (restore stats + run energy).
	private static final int POOL_DRINK_ANIM = 7305;

	// Object ids, scanned for in the loaded scene (layout-independent).
	private static final int[] POOL_IDS = {
		ObjectID.ORNATE_POOL_OF_REJUVENATION, ObjectID.FANCY_POOL_OF_REJUVENATION,
		ObjectID.POOL_OF_REJUVENATION, ObjectID.POOL_OF_REVITALISATION
	};
	private static final int[] NEXUS_IDS = {
		ObjectID.PORTAL_NEXUS, ObjectID.PORTAL_NEXUS_33409, ObjectID.PORTAL_NEXUS_33410
	};
	private static final int BANK_BOOTH_ID = ObjectID.BANK_BOOTH_16700;   // Lunar Isle booth

	/** Ticks between re-issued clicks so we don't spam while the toon is walking/casting. */
	private static final int ACTION_DEBOUNCE_TICKS = 5;

	private final Client client;
	private final MirrorState mirror;
	private final ClickRouter router;
	private final BankProfile profile;

	private Phase phase = Phase.IDLE;
	private int lastActionTick = Integer.MIN_VALUE / 2;
	private int openBankArriveTick = -1;   // tick we landed on Lunar Isle; gates the pre-click pause
	private int openBankDelay = 0;          // random 2-3 tick human beat before the first booth click
	private boolean restoredThisVisit = false;
	private String label = "";

	public BankRun(Client client, MirrorState mirror, ClickRouter router, BankProfile profile)
	{
		this.client = client; this.mirror = mirror; this.router = router; this.profile = profile;
	}

	/** Boss calls this when it decides a bank run is needed. */
	public void begin()
	{
		phase = Phase.TELE_TO_POH;
		restoredThisVisit = false;
		lastActionTick = Integer.MIN_VALUE / 2;
		openBankArriveTick = -1;
		openBankDelay = 0;
	}

	public void reset()            { phase = Phase.IDLE; restoredThisVisit = false; openBankArriveTick = -1; openBankDelay = 0; }
	/** True when the player is currently inside their POH -- the boss uses this to decide whether an
	 *  F7 start should kick off a bank run (starting in the POH means "go bank") vs. a normal start. */
	public boolean inPoh()         { return inMapRegion(POH_REGION); }
	public boolean active()        { return phase != Phase.IDLE && phase != Phase.READY; }
	public boolean bankReady()     { return phase == Phase.READY; }
	public Phase phase()           { return phase; }
	public String label()          { return label; }

	/**
	 * Advance the run one tick. Returns true while BankRun owns the tick (travelling / opening the bank), so
	 * the boss should take no other action; returns false when IDLE or READY (boss drives from there).
	 */
	public boolean step(int tick)
	{
		switch (phase)
		{
			case IDLE:
				return false;

			case TELE_TO_POH:
				if (inMapRegion(POH_REGION)) { phase = Phase.RESTORE_POOL; restoredThisVisit = false; return true; }
				if (inMapRegion(POH_TRANSITION_REGION)) { label = "bank: entering house"; return true; }
				label = "bank: teleport to POH";
				if (tick - lastActionTick >= ACTION_DEBOUNCE_TICKS)
				{
					lastActionTick = tick;
					if (profile.teleportKind == BankProfile.TeleportKind.TELETAB)
						router.useInventoryItem(profile.teleTabItemId, profile.teleTabOption);
					else
						router.castWidget(profile.houseTeleWidgetId, profile.houseTeleOption, profile.houseTeleName);
				}
				return true;

			case RESTORE_POOL:
				if (playerAnim() == POOL_DRINK_ANIM) restoredThisVisit = true;   // latch the drink
				if (!profile.restoreAtPool || restoredThisVisit) { phase = Phase.NEXUS_TO_LUNAR; return true; }
				GameObject pool = SceneScanner.first(client, go -> isAnyId(go.getId(), POOL_IDS));
				if (pool == null) { label = "bank: waiting for pool"; return true; }
				interactDebounced(tick, pool, MenuAction.GAME_OBJECT_FIRST_OPTION, "Drink", "bank: drink pool");
				return true;

			case NEXUS_TO_LUNAR:
				if (inMapRegion(LUNAR_ISLE_REGION))
				{
					// Arrived on Lunar Isle. Clear the recent nexus-click debounce, then wait a short random
					// 2-3 tick beat before clicking the booth -- an instant on-arrival click looks robotic.
					phase = Phase.OPEN_BANK;
					lastActionTick = Integer.MIN_VALUE / 2;
					openBankArriveTick = tick;
					openBankDelay = ThreadLocalRandom.current().nextInt(3, 6);   // 3 - 5 ticks
					label = "bank: landed, pausing";
					return true;
				}
				GameObject nexus = SceneScanner.first(client, go -> isAnyId(go.getId(), NEXUS_IDS));
				if (nexus == null) { label = "bank: waiting for nexus"; return true; }
				interactDebounced(tick, nexus, MenuAction.GAME_OBJECT_FIRST_OPTION, profile.nexusOption,
					"bank: nexus -> " + profile.nexusOption);
				return true;

			case OPEN_BANK:
				if (WidgetReader.isBankOpen(client)) { phase = Phase.READY; label = "bank: open"; return true; }
				if (!inMapRegion(LUNAR_ISLE_REGION)) { label = "bank: arriving at bank"; return true; }
				if (tick - openBankArriveTick < openBankDelay) { label = "bank: landed, pausing"; return true; }
				// Open the SPECIFIC booth at profile.bankBoothTile when set (the scene has several Lunar booths;
				// firstById can grab one across the counter / unreachable). Fall back to first-by-id otherwise.
				GameObject booth = (profile.bankBoothTile != null)
					? SceneScanner.first(client, go -> go.getId() == BANK_BOOTH_ID
						&& profile.bankBoothTile.equals(go.getWorldLocation()))
					: SceneScanner.firstById(client, BANK_BOOTH_ID);
				if (booth == null) { label = "bank: waiting for booth"; return true; }
				interactDebounced(tick, booth, MenuAction.GAME_OBJECT_SECOND_OPTION, "Bank", "bank: open booth");
				return true;

			case READY:
			default:
				return false;
		}
	}

	private void interactDebounced(int tick, GameObject go, MenuAction action, String option, String lbl)
	{
		label = lbl;
		if (tick - lastActionTick < ACTION_DEBOUNCE_TICKS) return;
		lastActionTick = tick;
		router.interactGameObject(go, action, option);
	}

	private int playerAnim()
	{
		Player p = mirror.getLocalPlayer();
		return p == null ? -1 : p.getAnimation();
	}

	private boolean inMapRegion(int regionId)
	{
		int[] rs = client.getMapRegions();
		if (rs == null) return false;
		for (int r : rs) if (r == regionId) return true;
		return false;
	}

	private static boolean isAnyId(int id, int[] ids)
	{
		for (int x : ids) if (x == id) return true;
		return false;
	}
}
