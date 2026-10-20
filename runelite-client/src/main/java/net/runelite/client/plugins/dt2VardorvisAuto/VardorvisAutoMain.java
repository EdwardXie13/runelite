package net.runelite.client.plugins.dt2VardorvisAuto;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.widgets.Widget;
import java.util.ArrayList;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.combat.ThrallManager;
import net.runelite.client.plugins.autoBossCore.combat.Hazard;
import net.runelite.client.plugins.autoBossCore.combat.PrayerSwapper;
import net.runelite.client.plugins.autoBossCore.consume.EatDelay;
import net.runelite.client.plugins.autoBossCore.state.WidgetReader;
import net.runelite.client.plugins.autoBossCore.combat.SchedulingTileDodger;
import net.runelite.client.plugins.autoBossCore.consume.ConsumptionEngine;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;
import net.runelite.client.plugins.autoBossCore.phase.PhaseTracker;
import net.runelite.client.plugins.autoBossCore.state.InstanceCoords;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.plugins.autoBossCore.trip.TripState;
import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

/**
 * Vardorvis strategy. Owns the tick loop for the fight: hazard scanning, prayer camping,
 * projectile-driven prayer swaps, dodge decisions, supply consumption, attack maintenance.
 * <p>
 * Captcha mechanic (anim 10342 / 10343) is recognised but automation suspends during it
 * -- user handles the prompt manually until we build a solver.
 */
@Slf4j
@Singleton
public class VardorvisAutoMain
{
	public enum VardPhase { OUT_OF_FIGHT, IDLE, FIGHT, CAPTCHA }

	// --- Services ---------------------------------------------------
	private final Client client;
	private final MirrorState mirror;
	private final ClickRouter router;
	private final SchedulingTileDodger dodger;
	private final PrayerSwapper prayers;

	// --- Boss state -------------------------------------------------
	private final ConsumptionEngine engine;
	private final ThrallManager thrall;
	// Melee combat-boost (divine super combat preferred, else super combat). Divine runs on
	// a 5-min timer; regular re-doses below VardorvisAutoIDs.SUPER_COMBAT_REDRINK_MIN_BOOST.
	private final net.runelite.client.plugins.autoBossCore.consume.CombatBoostTracker superCombatBoost =
		new net.runelite.client.plugins.autoBossCore.consume.CombatBoostTracker();
	private final TripState trip = new TripState();
	private final PhaseTracker<VardPhase> phase;

	private volatile boolean active = false;

	private NPC vardorvis;
	private int lastHeadSwapTick = -100;
	private int lastAttackTick = -100;

	/** Dragon Hunter Lance reach (2 tiles orthogonal and diagonal). */
	private static final int WEAPON_REACH = 2;
	/** Weapon attack cooldown in ticks. */
	private static final int WEAPON_COOLDOWN = 5;

	/** Shared eat/combo-eat attack-delay tracker (see autoBossCore EatDelay). */
	private final EatDelay eatDelay = new EatDelay();

	/** Tick an IDLE-phase poke was fired. Used to drive explicit home-return afterward. */
	private int lastIdlePokeTick = -100;

	/** Latched once the boss HP bar reads 0 (the kill) so every prayer is dropped at death and
	 *  prayer camping does not flick them back on through the death animation. Reset between kills. */
	private boolean deathPrayersOff = false;
	/** Was Vardorvis present last tick? Drives the despawn backstop for prayers-off. */
	private boolean vardorvisWasPresent = false;

	/** Tendril NPC -> tick first observed. Used to pre-mark the axe-spawn tile. */
	private final Map<NPC, Integer> tendrilSpawnTick = new IdentityHashMap<>();

	/**
	 * True while the most recent planNextTile result came from the SCRIPTED outgoing
	 * return-home branch. Attack-weave must not preempt a scripted move -- the whole
	 * point of the script is that this specific HOME click must land THIS tick for the
	 * axe-skip timing to work.
	 */
	private boolean lastPlanWasScripted = false;

	/**
	 * Scripted return-home tick for an OUTGOING tendril (one whose 3x3 spawn footprint
	 * covers HOME). Set when such a tendril first appears; the planner force-clicks HOME
	 * on this tick, overriding its usual "wait for safe to be imminent" heuristic.
	 * User-confirmed: in OSRS, player movement resolves before the axe damages the tile,
	 * so a SAFE -> HOME click two ticks after the tendril spawn lands cleanly as the axe
	 * sweeps through HOME.
	 */
	private int scriptedHomeReturnTick = -100;

	// Instance-resolved tiles. Null until the player enters the arena region.
	private WorldPoint homeTile;
	private WorldPoint safeTile;
	private int lastResolveTick = -100;

	/** QTE widget ids we've already fired this captcha instance. Reset on captcha entry. */
	private final java.util.Set<Integer> qteClicked = new java.util.HashSet<>();

	// Walk-click dedupe: suppress re-sending the same destination tick-after-tick.
	private WorldPoint lastWalkTarget;
	private int lastWalkTick = -100;

	/** Per-run dedupe of NPC ids seen while Vardorvis himself was NOT found. */
	private final java.util.Set<Integer> loggedMissingNpcIds = new java.util.HashSet<>();
	private int lastDiagTick = -100;

	// Overlay slot the plugin owns; main just updates it.
	private BaseOverlay overlay;

	@Inject
	public VardorvisAutoMain(
		Client client,
		MirrorState mirror,
		ClickRouter router,
		SchedulingTileDodger dodger,
		PrayerSwapper prayers)
	{
		this.client  = client;
		this.mirror  = mirror;
		this.router  = router;
		this.dodger  = dodger;
		this.prayers = prayers;

		this.engine = new ConsumptionEngine(VardorvisSupply.PROFILE, mirror);
		this.thrall = new ThrallManager(VardorvisAutoIDs.THRALL_CAST_WIDGET, "Vardorvis thrall");

		this.phase = new PhaseTracker<>(VardPhase.OUT_OF_FIGHT)
			.on(VardPhase.CAPTCHA, this::isCaptchaAnim,
				"captcha anim detected")
			.on(VardPhase.FIGHT, () -> vardorvis != null && isVardorvisActive() && !isCaptchaAnim(),
				"vardorvis alive, active markers present")
			.on(VardPhase.IDLE,  () -> vardorvis != null && !isVardorvisActive() && !isCaptchaAnim(),
				"vardorvis present but dormant")
			.on(VardPhase.OUT_OF_FIGHT, () -> vardorvis == null && !isCaptchaAnim(),
				"no boss, no captcha");
	}

	public void bindOverlay(BaseOverlay o) { this.overlay = o; }

	public boolean isActive()      { return active; }
	public void    start()
	{
		active = true;
		loggedMissingNpcIds.clear();
	}
	public void    stop()          { active = false;  }
	public VardPhase currentPhase(){ return phase.current(); }
	public TripState trip()        { return trip; }

	// -----------------------------------------------------------------
	// Tick loop.
	// -----------------------------------------------------------------

	public void onGameTick()
	{
		int tick = mirror.getTickCounter();

		vardorvis = findVardorvis();

		// Death / despawn backstop: when the boss vanishes, force every prayer off (the HP-0 trigger
		// below is the primary, earlier cue -- this catches the case where we miss the 0 read).
		if (vardorvisWasPresent && vardorvis == null) turnPrayersOff(tick);
		vardorvisWasPresent = (vardorvis != null);

		phase.update(tick);

		// Reset per-kill latches whenever there is no fight in progress, so the next kill starts
		// clean: the IDLE-poke marker (so the wake poke is not pre-empted by a stale home-return)
		// and the death-prayers latch (so camping re-arms for the new fight).
		if (phase.current() == VardPhase.OUT_OF_FIGHT)
		{
			lastIdlePokeTick = -100;
			deathPrayersOff = false;
			eatDelay.reset();
			thrall.reset();
		}

		// Primary death trigger: the boss HP bar reads 0 at the kill, a few ticks before the NPC
		// despawns. Turn ALL prayers off once (overheads + Piety) so we stop draining through the
		// death animation; deathPrayersOff then gates camping so nothing flicks them back on.
		if (!deathPrayersOff && vardorvis != null && bossCurrentHp() == 0)
		{
			deathPrayersOff = true;
			turnPrayersOff(tick);
		}

		// 1. Always track hazards -- even when inactive, so capture + overlay stay useful.
		// IMPORTANT: clear EVERY tick before re-projecting. Each of the push* methods below
		// iterates live NPCs / graphics objects and re-adds hazards each tick, so wiping
		// and rebuilding is correct AND avoids a phantom-hazard trap: static-tile hazards
		// only expire when their atTick passes, so future-tick projections from a now-dead
		// axe/tendril would otherwise linger and falsely flag tiles as unsafe long after
		// the real axe has moved or despawned.
		dodger.clearAll();
		pushAxeHazards(tick);
		pushSpikeHazards(tick);
		watchHeadProjectile(tick);

		dodger.onGameTick(tick);
		prayers.onGameTick(tick);

		updateOverlay();

		// Diagnostic heartbeat every ~5 ticks while in arena.
		if (isInArenaTemplate() && tick - lastDiagTick >= 5)
		{
			lastDiagTick = tick;
		}

		if (!active) return;

		// 2. Captcha phase: click all visible QTE_MODEL children (cluster-ordered, capped per tick).
		if (phase.current() == VardPhase.CAPTCHA)
		{
			if (phase.ticksInPhase(tick) == 0) qteClicked.clear();
			// Human-like 2-tick reaction delay before firing. The clicks still finish well
			// inside the captcha damage window.
			int CAPTCHA_DELAY_TICKS = 3;
			if (phase.ticksInPhase(tick) < CAPTCHA_DELAY_TICKS)
			{
				if (overlay != null) overlay.setStep("captcha -- reacting");
				return;
			}
			handleCaptcha(tick);
			return;
		}

		// 3. Movement planning: home-tile first with picked dodge tile, fallback to pure dodger.
		resolveInstanceTilesIfNeeded(tick);
		WorldPoint me = mirror.getPlayerTile();
		if (me != null)
		{
			WorldPoint step = planNextTile(me, tick);
			if (step != null && !step.equals(me))
			{
				// Attack-weaving: only safe when the move is TRULY voluntary -- i.e., the
				// current tile is not about to be hit. If the current tile is dangerous in
				// the next couple of ticks, the step is an EMERGENCY/PROACTIVE dodge even
				// when its destination happens to be the home tile (e.g. dodging SAFE ->
				// HOME while the axe is sweeping across SAFE). Preempting that with an
				// attack keeps us on the dying tile and gets us hit.
				boolean isHomeReturn = step.equals(homeTile);
				boolean curIsSafetySide = dodger.isTileDangerousAt(me, tick + 1)
				                       || dodger.isTileDangerousAt(me, tick + 2);
				// Attack-weave is NEVER allowed to preempt a scripted move -- the HOME
				// click has to land THIS tick for the axe-skip timing to work.
				if (isHomeReturn && !curIsSafetySide && !lastPlanWasScripted
					&& phase.current() == VardPhase.FIGHT && canWeaveAttack(tick))
				{
					router.attackNpc(vardorvis);
					lastAttackTick = tick;
					if (overlay != null) overlay.setStep("weave attack (walk pending)");
					return;
				}

				// Dedupe: don't re-send the same destination every tick while we're walking there.
				boolean sameTarget = step.equals(lastWalkTarget) && tick - lastWalkTick <= 3;
				if (!sameTarget)
				{
					router.walkTo(step);
					lastWalkTarget = step;
					lastWalkTick = tick;
					if (overlay != null) overlay.setStep("walk -> " + step);
				}
				return;
			}
			// On home / staying put: clear the walk dedupe so a future walk fires promptly.
			if (step == null && me.equals(homeTile)) { lastWalkTarget = null; }
		}

		// 4. Prayer camping. In FIGHT: Protect from Melee + offensive Piety. In IDLE (dormant boss,
		// about to poke): bring Protect from Melee up EARLY so the overhead is already active when we
		// wake him. Suppressed once dead (deathPrayersOff) so we don't flick prayers back on.
		if (!deathPrayersOff)
		{
			if (phase.current() == VardPhase.FIGHT)
			{
				prayers.ensureOn(tick, Prayer.PROTECT_FROM_MELEE, Prayer.PIETY);
			}
			else if (phase.current() == VardPhase.IDLE)
			{
				prayers.ensureOn(tick, Prayer.PROTECT_FROM_MELEE);
			}
		}

		// 5. Supply consumption.
		ConsumptionEngine.Request req = buildConsumptionRequest();
		ConsumptionEngine.Result res = engine.evaluate(req);
		applyConsumption(res);
		// DPS thrall upkeep (6 prayer/cast, ~magic-level ticks): recast ASAP once expired, only in FIGHT,
		// only when consumption didn't use the tick and we're idle between swings -- so it can't cancel one.
		if (!res.hasAction() && tick - lastAttackTick > 2
			&& thrall.maintain(tick, mirror, router, thrallConditionsOk()))
		{
			lastAttackTick = tick;   // the cast consumes the action tick -> step 6 won't also attack
		}

		// 6. Attack. Works for both IDLE (poke to wake) and FIGHT. In IDLE the attack menuAction
		// auto-walks us to Vardorvis and pokes him; we keep poking until the active GameObjects show
		// he is awake (isVardorvisActive -> FIGHT), at which point the planner runs us back to camp.
		if (phase.current() == VardPhase.FIGHT || phase.current() == VardPhase.IDLE)
		{
			boolean idle = phase.current() == VardPhase.IDLE;
			Player player = mirror.getLocalPlayer();
			if (player != null && player.getInteracting() != vardorvis
				&& !eatDelay.attackBlocked(tick)
				&& tick - lastAttackTick > 2)
			{
				router.attackNpc(vardorvis);
				lastAttackTick = tick;
				if (idle) lastIdlePokeTick = tick;
				if (overlay != null) overlay.setStep(idle ? "poking vardorvis awake" : "attacking vardorvis");
			}
		}
	}

	// -----------------------------------------------------------------
	// Hazard scanning.
	// -----------------------------------------------------------------

	private void pushAxeHazards(int tick)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return;

		// Live axe NPCs. Project the full REMAINING flight path (not just the generic
		// moving-npc 3-tick lookahead) by walking the 3x3 footprint one tile per tick in
		// the predicted direction. This matters once the tendril despawns -- without an
		// explicit step-by-step projection, the arena-scan fallback can pick a tile that
		// LOOKS clear at tick+1/tick+2 but will in fact be clipped by the axe shortly.
		//
		// We ALSO add a shorter moving-npc hazard so other parts of the dodger that read
		// through the NPC-based hazard path still see the axe.
		for (NPC n : npcs)
		{
			if (n == null) continue;
			if (n.getId() != VardorvisAutoIDs.NPC_FLYING_AXE) continue;
			dodger.addHazard(Hazard.movingNpc(n, VardorvisAutoIDs.AXE_FLIGHT_PROJECT_TICKS, "axe"));

			WorldPoint anchor = n.getWorldLocation();
			if (anchor == null) continue;

			// Direction = OBSERVED velocity from NpcTrack. No predictor needed here --
			// a mid-flight axe passing near the arena center would make a position-based
			// predictor flip sign and produce bogus directions. If velocity is 0 (axe
			// just spawned, hasn't moved yet), skip: the tendril projection -- which runs
			// from the stationary tendril anchor -- already covers this tick's hazards.
			MirrorState.NpcTrack track = mirror.trackOf(n);
			if (track == null || track.current == null || track.last == null) continue;
			int dxDir = Integer.signum(track.dx());
			int dyDir = Integer.signum(track.dy());
			if (dxDir == 0 && dyDir == 0) continue;  // just spawned -- tendril projection covers it

			int axeX = anchor.getX();
			int axeY = anchor.getY();
			int planeZ = anchor.getPlane();
			for (int step = 0; step < VardorvisAutoIDs.AXE_FLIGHT_PROJECT_TICKS; step++)
			{
				int originX = axeX + dxDir * step;
				int originY = axeY + dyDir * step;
				WorldPoint originInstance = new WorldPoint(originX, originY, planeZ);
				WorldPoint originTemplate = InstanceCoords.toTemplate(client, originInstance);
				if (originTemplate != null)
				{
					int tMinX = originTemplate.getX(), tMaxX = tMinX + 2;
					int tMinY = originTemplate.getY(), tMaxY = tMinY + 2;
					boolean fullyOutside = tMaxX < VardorvisAutoIDs.ARENA_X_MIN
						|| tMinX > VardorvisAutoIDs.ARENA_X_MAX
						|| tMaxY < VardorvisAutoIDs.ARENA_Y_MIN
						|| tMinY > VardorvisAutoIDs.ARENA_Y_MAX;
					if (fullyOutside) break;
				}
				java.util.List<WorldPoint> footprint = new java.util.ArrayList<>(9);
				for (int ddx = 0; ddx <= 2; ddx++)
					for (int ddy = 0; ddy <= 2; ddy++)
						footprint.add(new WorldPoint(originX + ddx, originY + ddy, planeZ));
				dodger.addHazard(Hazard.staticTiles(footprint, tick + step, "live-axe-step" + step));
			}
		}

		// Tendril (12225) spawns → axe appears on same tile AXE_SPAWN_DELAY_TICKS later.
		// Push a static-tile hazard for that moment so we're walked off BEFORE spawn.
		for (NPC n : npcs)
		{
			if (n == null) continue;
			if (n.getId() != VardorvisAutoIDs.NPC_LARGE_TENDRIL) continue;
			WorldPoint anchor = n.getWorldLocation();
			Integer spawn = tendrilSpawnTick.get(n);
			boolean firstSight = (spawn == null);
			if (firstSight)
			{
				spawn = tick;
				tendrilSpawnTick.put(n, spawn);

				// Outgoing-tendril detection: if its 3x3 spawn footprint covers HOME, this
				// is a "spawn-on-us" tendril whose axe will depart SE. Schedule the scripted
				// HOME return at spawn + 2, i.e. the tick AFTER the proactive SAFE click
				// lands. The planner will force-click HOME on that tick.
				if (anchor != null && homeTile != null)
				{
					WorldPoint tendrilTemplate = InstanceCoords.toTemplate(client, anchor);
					if (tendrilTemplate != null)
					{
						int tMinX = tendrilTemplate.getX(), tMaxX = tMinX + 2;
						int tMinY = tendrilTemplate.getY(), tMaxY = tMinY + 2;
						int hx = VardorvisAutoIDs.HOME_TEMPLATE.getX();
						int hy = VardorvisAutoIDs.HOME_TEMPLATE.getY();
						boolean coversHome = hx >= tMinX && hx <= tMaxX && hy >= tMinY && hy <= tMaxY;
						if (coversHome)
						{
							scriptedHomeReturnTick = tick + 2;
						}
					}
				}
			}

			// Tendril and axe are always 3x3, SW-anchored. Axes travel in a deterministic
			// line from their spawn position toward the opposite side of the arena:
			//   NE spawn -> SW,  NW spawn -> SE,  W spawn -> E,  E spawn -> W, etc.
			// We classify the direction by the tendril's position RELATIVE TO HOME, then
			// project the axe's 3x3 footprint along that line for each tick of its flight,
			// marking each tick's tiles as hazardous. This gives the dodger a full "line of
			// fire" rather than just the spawn tiles.
			int axeAt = spawn + VardorvisAutoIDs.AXE_SPAWN_DELAY_TICKS;
			if (anchor != null && axeAt >= tick)
			{
				int[] dir = predictAxeDirection(anchor);
				int dxDir = dir[0], dyDir = dir[1];
				int planePlane = anchor.getPlane();
				int axeX = anchor.getX();
				int axeY = anchor.getY();

				for (int step = 0; step < VardorvisAutoIDs.AXE_FLIGHT_PROJECT_TICKS; step++)
				{
					int originX = axeX + dxDir * step;
					int originY = axeY + dyDir * step;
					// Convert the 3x3's bounding box to template space to clip against arena bounds.
					WorldPoint originInstance = new WorldPoint(originX, originY, planePlane);
					WorldPoint originTemplate = InstanceCoords.toTemplate(client, originInstance);
					if (originTemplate != null)
					{
						int tMinX = originTemplate.getX(), tMaxX = tMinX + 2;
						int tMinY = originTemplate.getY(), tMaxY = tMinY + 2;
						boolean fullyOutside = tMaxX < VardorvisAutoIDs.ARENA_X_MIN
							|| tMinX > VardorvisAutoIDs.ARENA_X_MAX
							|| tMaxY < VardorvisAutoIDs.ARENA_Y_MIN
							|| tMinY > VardorvisAutoIDs.ARENA_Y_MAX;
						if (fullyOutside) break;  // axe has exited the arena, stop projecting
					}
					java.util.List<WorldPoint> footprint = new java.util.ArrayList<>(9);
					for (int ddx = 0; ddx <= 2; ddx++)
						for (int ddy = 0; ddy <= 2; ddy++)
							footprint.add(new WorldPoint(originX + ddx, originY + ddy, planePlane));
					dodger.addHazard(Hazard.staticTiles(footprint, axeAt + step, "axe-path-step" + step));
				}

				if (firstSight)
				{
				}
			}
		}

		// Prune tracked tendrils that have despawned.
		Set<NPC> live = new HashSet<>(npcs);
		tendrilSpawnTick.keySet().retainAll(live);
	}

	private void pushSpikeHazards(int tick)
	{
		Iterable<GraphicsObject> gfx = client.getGraphicsObjects();
		if (gfx == null) return;
		for (GraphicsObject g : gfx)
		{
			if (g == null) continue;
			int id = g.getId();
			boolean isIndicator = id == VardorvisAutoIDs.GFX_SPIKE_INDICATOR;
			boolean isActive    = id == VardorvisAutoIDs.GFX_SPIKE_ACTIVE;
			if (!isIndicator && !isActive) continue;

			LocalPoint lp = g.getLocation();
			if (lp == null) continue;
			// fromLocal (NOT fromLocalInstance) -- hazard tiles must be in INSTANCE
			// coords to match mirror.getPlayerTile() (which comes from
			// player.getWorldLocation, instance space). fromLocalInstance returns the
			// TEMPLATE coord via chunk lookup, which would silently never match the
			// player's instance tile and leave spike dodging broken.
			WorldPoint tile = WorldPoint.fromLocal(client, lp);
			if (tile == null) continue;

			if (isIndicator)
			{
				// Pre-sprout: hazard at the sprout tick.
				dodger.addHazard(Hazard.staticTile(tile,
					tick + VardorvisAutoIDs.SPIKE_SPROUT_TICKS, "spike-indicator"));
			}
			else
			{
				// Active spike -- dangerous NOW and for the next couple of ticks.
				// Mark it as hazard at tick, tick+1, tick+2 so the dodger won't path us onto it.
				dodger.addHazard(Hazard.staticTile(tile, tick,     "spike-active"));
				dodger.addHazard(Hazard.staticTile(tile, tick + 1, "spike-active"));
				dodger.addHazard(Hazard.staticTile(tile, tick + 2, "spike-active"));
			}
		}
	}

	/**
	 * When the head projectile is spawned, schedule a Protect-from-Missiles swap on the
	 * tick BEFORE impact and a swap back to Protect-from-Melee on the tick AFTER.
	 */
	private void watchHeadProjectile(int tick)
	{
		for (MirrorState.ProjectileRecord pr : mirror.getProjectileRecords())
		{
			if (pr.id != VardorvisAutoIDs.PROJ_HEAD) continue;
			int impactTick = pr.endTick;
			if (impactTick <= tick) continue;
			// Pray missiles 2 ticks before impact (more human-looking than tick-1 flicks).
			int swapTick = Math.max(tick, impactTick - 2);
			if (swapTick <= lastHeadSwapTick) continue;

			prayers.scheduleSwap(Prayer.PROTECT_FROM_MISSILES, swapTick);
			prayers.scheduleSwap(Prayer.PROTECT_FROM_MELEE,    impactTick + 1);
			lastHeadSwapTick = swapTick;
		}
	}

	// -----------------------------------------------------------------
	// Consumption.
	// -----------------------------------------------------------------

	/** Vardorvis thrall gate: only while actively fighting (awake, not a captcha/QTE window). Style via
	 *  VardorvisAutoIDs.THRALL_CAST_WIDGET. */
	private boolean thrallConditionsOk()
	{
		return phase.current() == VardPhase.FIGHT;
	}

	private ConsumptionEngine.Request buildConsumptionRequest()
	{
		ConsumptionEngine.Request.Builder b = ConsumptionEngine.Request.builder();

		// Eat only AFTER HP falls to the threshold -- topping off from near-full wastes food and each
		// eat delays our next attack (DPS loss). The engine's overheal cap still prevents overeating,
		// and its critical-HP floor (hpEatCriticalThreshold) still force-eats when we are about to die.
		boolean needFood = mirror.getHp() <= VardorvisSupply.PROFILE.hpEatThreshold;
		b.wantEat(needFood);
		b.allowComboEat(needFood);

		// Prayer: standalone sip below HALF of max (jittered); when eating (needFood) the swing is
		// already delayed, so piggyback a free top-up instead.
		ConsumptionEngine.requestPrayerSip(b, mirror, Math.max(1, mirror.getMaxPrayer() / 2),
			VardorvisSupply.PROFILE.prayerSipJitterWindow, needFood);

		if (mirror.getRunEnergy() <= VardorvisSupply.PROFILE.runEnergySipStam
			&& VardorvisSupply.PROFILE.hasPotion(SupplyProfile.DrinkKind.STAMINA))
		{
			b.opportunisticSip(SupplyProfile.DrinkKind.STAMINA);
		}

		// Melee combat-boost top-off (divine super combat preferred, else super combat; only
		// one is ever carried). Divine is refreshed on a 5-min timer; regular re-doses once
		// the remaining Strength boost drops below the boss threshold. Out of fight, reset the
		// timer so the next kill re-evaluates from scratch.
		if (vardorvis == null)
		{
			superCombatBoost.reset();
		}
		else
		{
			superCombatBoost.requestSips(b, mirror.getTickCounter(),
				client.getBoostedSkillLevel(net.runelite.api.Skill.STRENGTH),
				client.getRealSkillLevel(net.runelite.api.Skill.STRENGTH),
				SupplyProfile.DrinkKind.DIVINE_SUPER_COMBAT,
				SupplyProfile.DrinkKind.SUPER_COMBAT,
				VardorvisAutoIDs.SUPER_COMBAT_REDRINK_MIN_BOOST);
		}

		return b.build();
	}

	private void applyConsumption(ConsumptionEngine.Result r)
	{
		if (r == null || !r.hasAction()) return;
		if (r.primaryEat != null) router.useInventoryItem(r.primaryEat.itemId, "Eat");
		if (r.comboEat   != null) router.useInventoryItem(r.comboEat.itemId, "Eat");
		// Eating delays our next attack (food 3t, + combo food 2t). Record it so step 6 / the weave
		// don't fire a wasted click mid-delay.
		eatDelay.onConsumed(mirror.getTickCounter(), r);
		if (r.drink      != null)
		{
			SupplyProfile.PotionEntry pot = VardorvisSupply.PROFILE.potion(r.drink);
			if (pot == null) return;
			int sipId = findLowestDoseItem(pot);
			if (sipId > 0) router.useInventoryItem(sipId, "Drink");
			// Restart the divine super-combat 5-min timer when a divine dose went down.
			if (r.drink == SupplyProfile.DrinkKind.DIVINE_SUPER_COMBAT)
				superCombatBoost.onDivineSipped(mirror.getTickCounter());
		}
	}

	private int findLowestDoseItem(SupplyProfile.PotionEntry pot)
	{
		for (int i = 0; i < pot.doseIds.length; i++)
		{
			if (mirror.countItem(pot.doseIds[i]) > 0) return pot.doseIds[i];
		}
		return -1;
	}

	// -----------------------------------------------------------------
	// Helpers.
	// -----------------------------------------------------------------

	private boolean isCaptchaAnim()
	{
		if (vardorvis == null) return false;
		int a = vardorvis.getAnimation();
		return a == VardorvisAutoIDs.ANIM_CAPTCHA_PREP
			|| a == VardorvisAutoIDs.ANIM_CAPTCHA_SHOWN;
	}

	/** True if any of the Vardorvis active-marker GameObjects are in the scene. */
	private boolean isVardorvisActive()
	{
		Scene scene = client.getTopLevelWorldView().getScene();
		if (scene == null) return false;
		int plane = client.getTopLevelWorldView().getPlane();
		Tile[][][] tiles = scene.getExtendedTiles();
		if (tiles == null || plane < 0 || plane >= tiles.length) return false;
		Tile[][] planeTiles = tiles[plane];
		if (planeTiles == null) return false;
		for (Tile[] col : planeTiles)
		{
			if (col == null) continue;
			for (Tile t : col)
			{
				if (t == null) continue;
				GameObject[] gos = t.getGameObjects();
				if (gos == null) continue;
				for (GameObject g : gos)
				{
					if (g == null) continue;
					int id = g.getId();
					for (int marker : VardorvisAutoIDs.GOBJ_ACTIVE_MARKERS)
						if (id == marker) return true;
				}
			}
		}
		return false;
	}

	/** Locate Vardorvis in the arena. Prefers regular 12223; falls back to awakened if set. */

	/** True if the player is within WEAPON_REACH Chebyshev tiles of any Vardorvis tile. */
	private boolean isInWeaponRange(NPC npc)
	{
		if (npc == null) return false;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;
		WorldPoint anchor = npc.getWorldLocation();
		if (anchor == null) return false;
		int size = VardorvisAutoIDs.VARDORVIS_SIZE;
		int npcMinX = anchor.getX(), npcMaxX = anchor.getX() + size - 1;
		int npcMinY = anchor.getY(), npcMaxY = anchor.getY() + size - 1;
		int dx = Math.max(0, Math.max(npcMinX - me.getX(), me.getX() - npcMaxX));
		int dy = Math.max(0, Math.max(npcMinY - me.getY(), me.getY() - npcMaxY));
		return Math.max(dx, dy) <= WEAPON_REACH;
	}

	/** True if we can fire a (re-)attack this tick: not already interacting + cooldown is up + in range. */
	private boolean canWeaveAttack(int tick)
	{
		Player player = mirror.getLocalPlayer();
		if (player == null || vardorvis == null) return false;
		if (player.getInteracting() == vardorvis) return false;  // already attacking, no re-click needed
		if (eatDelay.attackBlocked(tick)) return false;           // inside an eat / combo-eat delay
		if (tick - lastAttackTick < WEAPON_COOLDOWN) return false;
		return isInWeaponRange(vardorvis);
	}

	private NPC findVardorvis()
	{
		int awakened = VardorvisAutoIDs.NPC_VARDORVIS_AWAKENED;
		return awakened > 0
			? findNpcAny(VardorvisAutoIDs.NPC_VARDORVIS, awakened)
			: findNpcAny(VardorvisAutoIDs.NPC_VARDORVIS);
	}

	/** Return the first NPC whose id matches any of {@code ids}. Null if none present. */
	private NPC findNpcAny(int... ids)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		for (NPC n : npcs)
		{
			if (n == null) continue;
			for (int id : ids) if (n.getId() == id) return n;
		}

		// Diagnostic: log the ids we DO see so we can tell if the expected ones are wrong.
		if (active && isInArenaTemplate())
		{
			for (NPC n : npcs)
			{
				if (n == null) continue;
				if (!loggedMissingNpcIds.add(n.getId())) continue;
				String name = n.getName();
			}
		}
		return null;
	}

	private void updateOverlay()
	{
		if (overlay == null) return;
		overlay.setStat("active",  active ? "yes" : "no");
		overlay.setStat("phase",   phase.current().name());
		overlay.setStat("hp",      mirror.getHp()     + "/" + mirror.getMaxHp());
		overlay.setStat("pray",    mirror.getPrayer() + "/" + mirror.getMaxPrayer());
		overlay.setStat("tick",    String.valueOf(mirror.getTickCounter()));
	}

	// -----------------------------------------------------------------
	// Movement planning (home-tile aware).
	// -----------------------------------------------------------------

	/**
	 * Resolve the HOME / DODGE template coordinates to their actual instance tiles.
	 * Instance layout can shift between kills; re-resolve whenever we're in-arena.
	 */
	private void resolveInstanceTilesIfNeeded(int tick)
	{
		// Inside an instance, player.getWorldLocation().getRegionID() returns the DYNAMIC
		// instance region (e.g. 14xxx), not the template region (4405). Convert to template
		// space first so the gate actually fires.
		WorldPoint pt = mirror.getPlayerTile();
		WorldPoint tpl = InstanceCoords.toTemplate(client, pt);
		boolean inArena = tpl != null && tpl.getRegionID() == VardorvisAutoIDs.ARENA_REGION;
		if (!inArena) return;
		if (homeTile != null && tick - lastResolveTick < 20) return;

		homeTile = resolveFirst(VardorvisAutoIDs.HOME_TEMPLATE);
		safeTile = resolveFirst(VardorvisAutoIDs.SAFE_TILE_TEMPLATE);
		lastResolveTick = tick;
	}

	/** Template-space check: are we in the Vardorvis arena? */
	private boolean isInArenaTemplate()
	{
		WorldPoint tpl = InstanceCoords.toTemplate(client, mirror.getPlayerTile());
		return tpl != null && tpl.getRegionID() == VardorvisAutoIDs.ARENA_REGION;
	}

	private WorldPoint resolveFirst(WorldPoint template)
	{
		return InstanceCoords.fromTemplate(client, template);
	}

	/** Current boss HP from the in-fight health bar widget; -1 when the bar is not showing. */
	private int bossCurrentHp()
	{
		return WidgetReader.bossHp(client);
	}

	/** Turn ALL our prayers off (the three overheads AND offensive Piety) and drop any pending swaps. */
	private void turnPrayersOff(int tick)
	{
		prayers.clearScheduled();
		java.util.Set<Prayer> manageOff = new java.util.HashSet<>(PrayerSwapper.OVERHEAD_SET());
		manageOff.add(Prayer.PIETY);
		prayers.ensureOnlyOn(tick, java.util.Collections.emptySet(), manageOff);
	}

	/**
	 * Decide where the player should be next tick.
	 * <ol>
	 *   <li>Current tile dangerous within short horizon → pick the chosen dodge tile
	 *       (opposite side of Vardorvis's lean). Fall back to free-form dodge if that
	 *       dodge tile is also dangerous.</li>
	 *   <li>Not on home + home safe → return to home for DPS.</li>
	 *   <li>Otherwise stay (returns null).</li>
	 * </ol>
	 */
	private WorldPoint planNextTile(WorldPoint current, int tick)
	{
		// If we've never been in-arena this trip, defer to the dodger entirely.
		if (homeTile == null || safeTile == null)
		{
			return dodger.computeDodge(current, null, 3).orElse(null);
		}

		// Scripted HOME return for an outgoing tendril (one that spawned on us). This
		// fires exactly once per outgoing tendril, on the tick after our proactive SAFE
		// dodge. Trust OSRS tick-ordering to resolve player movement before the axe
		// damages HOME. Overrides the usual hazard-based logic.
		lastPlanWasScripted = false;  // default each call; set true below if we return a scripted step
		if (tick == scriptedHomeReturnTick)
		{
			scriptedHomeReturnTick = -100;  // consume
			WorldPoint curTpl = InstanceCoords.toTemplate(client, current);
			boolean alreadyHome = curTpl != null && curTpl.equals(VardorvisAutoIDs.HOME_TEMPLATE);
			if (!alreadyHome)
			{
				lastPlanWasScripted = true;
				return homeTile;
			}
		}

		// Two-tile dance: HOME <-> SAFE. By standing on the west edge (HOME) with the dodge
		// tile diagonally SE (SAFE), only two-corner tendrils threaten us. The two tiles
		// are rarely bad in the same tick; when they are, we fall back to arena-scan.
		//
		// Two-mode trigger:
		//
		//   EMERGENCY  (curBadT1): cur tile is dangerous NEXT tick. Must move this tick
		//                          regardless of destination. We'll pick a target if one
		//                          is clear at arrival, else arena-scan.
		//
		//   PROACTIVE  (curBadT2 AND target clear at tick+1): cur tile is dangerous in
		//                          two ticks and the OPPOSITE tile is clear at tick+1.
		//                          Move NOW so we arrive at the destination a full tick
		//                          BEFORE the axe gets to our current tile. This is the
		//                          correct timing for OSRS tick-collision -- a hit can
		//                          register during the same tick we try to walk off.
		//
		//   WAIT  (curBadT2 but target bad at tick+1): cur tile is dangerous in two ticks
		//                          but the destination is also dangerous NEXT tick. Any
		//                          dodge now would land us in a hazard. Stay this tick;
		//                          the next planner call will replan with fresh info.
		//
		//   homeImminent = tick+1..tick+3. Used ONLY by the home-return decision.
		boolean curBadT1       = dodger.isTileDangerousAt(current, tick + 1);
		boolean curBadT2       = dodger.isTileDangerousAt(current, tick + 2);
		boolean safeBadArrival = dodger.isTileDangerousAt(safeTile, tick + 1);
		boolean homeBadArrival = dodger.isTileDangerousAt(homeTile, tick + 1);
		boolean homeImminent   = homeBadArrival
		                       || dodger.isTileDangerousAt(homeTile, tick + 2)
		                       || dodger.isTileDangerousAt(homeTile, tick + 3);

		WorldPoint curTemplate = InstanceCoords.toTemplate(client, current);
		boolean onHome = curTemplate != null && curTemplate.equals(VardorvisAutoIDs.HOME_TEMPLATE);
		boolean onSafe = curTemplate != null && curTemplate.equals(VardorvisAutoIDs.SAFE_TILE_TEMPLATE);

		// The OTHER named tile that we'd dodge TO from current.
		WorldPoint target        = onHome ? safeTile : (onSafe ? homeTile : null);
		boolean    targetBadT1   = onHome ? safeBadArrival : (onSafe ? homeBadArrival : true);

		boolean emergency = curBadT1;
		boolean proactive = curBadT2 && target != null && !targetBadT1;
		boolean mustMove  = emergency || proactive;

		if (mustMove)
		{
			// Dump the hazard tiles for diagnostics.
			java.util.Set<WorldPoint> hazTiles = dodger.dumpDangerSetAt(tick + 1);
			java.util.List<WorldPoint> hazTemplate = new java.util.ArrayList<>();
			for (WorldPoint w : hazTiles) hazTemplate.add(InstanceCoords.toTemplate(client, w));

			// Pick the OTHER tile between home and safe, provided it is clean at arrival.
			if (!safeBadArrival && (onHome || !onSafe))
			{
				return safeTile;
			}
			if (!homeBadArrival && (onSafe || !onHome))
			{
				return homeTile;
			}

			// Both named tiles unsafe at arrival. This shouldn't fire for a proactive move
			// (the gate above checks target clear), only for a true emergency where no
			// preset tile is clean at tick+1. Fall back to arena-scan.
			WorldPoint scanned = findClosestSafeArenaTile(current, tick);
			if (scanned != null)
			{
				return scanned;
			}

			WorldPoint ff = dodger.computeDodge(current, null, 1).orElse(null);
			return ff;
		}

		// Current is safe for the next tick. Return to the camp tile when it's safe -- but ONLY once
		// Vardorvis is actually AWAKE (active GameObjects present). While he is still dormant we are
		// poking him to wake him; running back to camp then cancels the poke's walk and leaves us
		// circling between the boss and camp. isVardorvisActive() is the awake check -- the same
		// markers that drive the FIGHT phase.
		boolean mayReturnHome = isVardorvisActive();
		if (!onHome && !homeImminent && mayReturnHome)
		{
			return homeTile;
		}
		return null;
	}

	/**
	 * Direction vector the axe will travel in, given its spawn (= tendril) SW anchor.
	 * Classified against the player's HOME tile: axes always move from their spawn
	 * toward the opposite arena position, so direction = -sign(tendrilRelativeToHome).
	 * Uses the tendril's 3x3 CENTER for the comparison to avoid off-by-one issues at
	 * the x==home.x boundary.
	 */
	private int[] predictAxeDirection(WorldPoint tendrilSW)
	{
		if (tendrilSW == null) return new int[]{0, 0};
		// Classify tendril position relative to the ARENA CENTER (not home -- home is at
		// the north edge). Axes travel from spawn edge toward the opposite side, crossing
		// the center, so direction = -sign(tendrilRelativeToCenter).
		WorldPoint tendrilTemplate = InstanceCoords.toTemplate(client, tendrilSW);
		if (tendrilTemplate == null) return new int[]{0, 0};
		int tcx = tendrilTemplate.getX() + 1;    // tendril 3x3 center x (template space)
		int tcy = tendrilTemplate.getY() + 1;    // tendril 3x3 center y
		int cx  = VardorvisAutoIDs.ARENA_CENTER_TEMPLATE.getX();
		int cy  = VardorvisAutoIDs.ARENA_CENTER_TEMPLATE.getY();
		int dx = Integer.signum(tcx - cx);
		int dy = Integer.signum(tcy - cy);
		return new int[]{-dx, -dy};
	}

	/**
	 * Full-arena fallback: scan every tile in the 11x11 Vardorvis arena and pick the one
	 * closest (Chebyshev distance) to {@code from} that is safe at tick+1 AND tick+2.
	 * Used when all preset dodge tiles (safe-space tiles and home) are compromised by
	 * overlapping hazards (e.g. spike indicator sprouting on home while an axe lane is
	 * active on both sides).
	 */
	private WorldPoint findClosestSafeArenaTile(WorldPoint from, int tick)
	{
		if (from == null || homeTile == null) return null;
		int dxOffset = homeTile.getX() - VardorvisAutoIDs.HOME_TEMPLATE.getX();
		int dyOffset = homeTile.getY() - VardorvisAutoIDs.HOME_TEMPLATE.getY();
		WorldPoint best = null;
		int bestDist = Integer.MAX_VALUE;
		for (int tx = VardorvisAutoIDs.ARENA_X_MIN; tx <= VardorvisAutoIDs.ARENA_X_MAX; tx++)
		{
			for (int ty = VardorvisAutoIDs.ARENA_Y_MIN; ty <= VardorvisAutoIDs.ARENA_Y_MAX; ty++)
			{
				WorldPoint cand = new WorldPoint(tx + dxOffset, ty + dyOffset, from.getPlane());
				if (cand.equals(from)) continue;
				if (dodger.isTileDangerousAt(cand, tick + 1)) continue;
				if (dodger.isTileDangerousAt(cand, tick + 2)) continue;
				int dist = Math.max(
					Math.abs(cand.getX() - from.getX()),
					Math.abs(cand.getY() - from.getY()));
				if (dist < bestDist)
				{
					bestDist = dist;
					best = cand;
				}
			}
		}
		return best;
	}

	// -----------------------------------------------------------------
	// Captcha solver.
	// -----------------------------------------------------------------

	/**
	 * Click ALL visible QTE_MODEL widgets, picked in a cluster-aware order: start at the top-left-most
	 * unclicked widget, then chain to its nearest unclicked neighbour. The destroys are STAGGERED on the
	 * click scheduler (~150-260ms jittered gaps), so one pass sweeps the whole captcha like a human instead
	 * of bursting a fixed group per tick.
	 */
	private void handleCaptcha(int tick)
	{
		List<int[]> visible = new ArrayList<>();  // [widgetId, cx, cy]
		for (int id : VardorvisAutoIDs.QTE_CHILD_WIDGET_IDS)
		{
			if (qteClicked.contains(id)) continue;
			Widget w = client.getWidget(id);
			if (w == null || w.isHidden()) continue;
			Point c = w.getCanvasLocation();
			if (c == null) { visible.add(new int[]{id, 0, 0}); continue; }
			visible.add(new int[]{id, c.getX() + w.getWidth() / 2, c.getY() + w.getHeight() / 2});
		}

		if (visible.isEmpty())
		{
			if (overlay != null) overlay.setStep("captcha -- waiting for widgets");
			return;
		}

		// Pick the top-left-most as the anchor, then nearest-neighbour chain.
		visible.sort((a, b) -> {
			int ay = a[2], by = b[2];
			if (ay != by) return Integer.compare(ay, by);
			return Integer.compare(a[1], b[1]);
		});
		List<int[]> order = new ArrayList<>();
		order.add(visible.remove(0));
		while (!visible.isEmpty())
		{
			int[] last = order.get(order.size() - 1);
			int bestIdx = 0, bestDist = Integer.MAX_VALUE;
			for (int i = 0; i < visible.size(); i++)
			{
				int[] v = visible.get(i);
				int dx = v[1] - last[1], dy = v[2] - last[2];
				int d = dx * dx + dy * dy;
				if (d < bestDist) { bestDist = d; bestIdx = i; }
			}
			order.add(visible.remove(bestIdx));
		}

		// Fire up to the per-tick cap, logging the clicked order with positions for audit.
		StringBuilder trace = new StringBuilder();
		// STAGGER ALL visible destroys this pass -- no per-tick cap or grouping. The 150-260ms jittered gaps
		// already space the clicks like a human sweep (mouse travel + click each), and the client sends each
		// packet as it fires (not batched to end-of-tick). qteClicked is marked synchronously so the next tick
		// won't re-queue a destroy still pending on the scheduler.
		long offset = java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 70);   // small reaction before the first
		for (int[] entry : order)
		{
			int id = entry[0];
			int cx = entry[1], cy = entry[2];
			router.destroyQteDelayed(id, offset);
			qteClicked.add(id);
			offset += java.util.concurrent.ThreadLocalRandom.current().nextInt(150, 260);   // mouse travel + click to the next
			if (trace.length() > 0) trace.append(" -> ");
			trace.append("w").append(id - 54591488).append("@(").append(cx).append(",").append(cy).append(")");
		}
		if (overlay != null) overlay.setStep("captcha -- fired " + order.size() + ", total done " + qteClicked.size());
	}
}
