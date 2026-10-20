package net.runelite.client.plugins.autoBossCore.combat;

import net.runelite.client.plugins.autoBossCore.state.MirrorState;

/**
 * Shared DPS-thrall caster for the DT2 bosses. A Greater thrall (Resurrect Greater Ghost / Skeleton /
 * Zombie on the Arceuus spellbook) is summoned by clicking its spell widget; each cast costs
 * {@value #PRAYER_COST} prayer points and the thrall lasts, AT MOST, 0.6 x magic level seconds ==
 * magic-level ticks (0.6s per tick). Thralls do not stack, so a recast is due only once the previous
 * one has (at latest) expired.
 *
 * <p>autoBossCore owns the timing + prayer-cost gate; the BOSS decides WHEN a cast is valid (region /
 * phase), because e.g. the Whisperer spends part of the fight in another realm/region where a cast is
 * pointless or impossible. The boss passes that decision as the {@code conditionsOk} flag to
 * {@link #maintain(int, MirrorState, ClickRouter, boolean)}.
 *
 * <p>Timing is ASAP: the instant the thrall is expired AND we hold the prayer points AND the boss says
 * conditions are ok, we recast -- accepting the one-tick cost, since thrall uptime is the whole point.
 */
public final class ThrallManager
{
	/** Prayer points consumed by a single Greater-thrall cast. */
	public static final int PRAYER_COST = 6;

	/** Item required in the inventory to cast any thrall (Book of the Dead). */
	public static final int REQUIRED_ITEM_ID = 25818;

	/** Arceuus spellbook widget param1 for each Greater thrall's "Cast" option. */
	public static final int WIDGET_GREATER_GHOST    = 14287043;   // magic
	public static final int WIDGET_GREATER_SKELETON = 14287044;   // ranged
	public static final int WIDGET_GREATER_ZOMBIE   = 14287045;   // melee

	/** Spellbook tab widget + the sprite id it shows on the ARCEUUS spellbook -- thralls live there, so we
	 *  can only cast one while this book is selected. */
	public static final int SPELLBOOK_WIDGET = 10551368;
	public static final int ARCEUUS_SPRITE   = 1711;

	private final int castWidgetId;
	private final int castableSpriteId;   // sprite the spell widget shows when castable; -1 = gate disabled
	private final String thrallName;

	private int lastCastTick = Integer.MIN_VALUE / 2;
	private int castDurationTicks = 1;
	private boolean everCast = false;

	public ThrallManager(int castWidgetId, String thrallName)
	{
		this.castWidgetId = castWidgetId;
		this.castableSpriteId = castableSpriteFor(castWidgetId);
		this.thrallName = thrallName == null ? "" : thrallName;
	}

	/**
	 * The sprite id a thrall's spell widget shows when the spell IS castable (runes + magic level present and
	 * no thrall currently active). A live reading that differs means we can't cast right now. Return -1 for a
	 * thrall whose castable sprite isn't known yet -> the sprite gate is disabled for it (see {@link #castable}).
	 */
	public static int castableSpriteFor(int castWidgetId)
	{
		switch (castWidgetId)
		{
			case WIDGET_GREATER_ZOMBIE:   return 2984;   // melee  -- confirmed
			case WIDGET_GREATER_SKELETON: return 2982;   // ranged -- confirmed
			case WIDGET_GREATER_GHOST:    return 2980;   // magic  -- confirmed
			default:                      return -1;
		}
	}

	/** True when the player is on the Arceuus spellbook (the only book with thralls). When the spellbook
	 *  widget isn't loaded (-1) this reads false, so we won't cast blind. */
	public static boolean onArceuus(MirrorState mirror)
	{
		return mirror.getWidgetSpriteId(SPELLBOOK_WIDGET) == ARCEUUS_SPRITE;
	}

	/** True when the spell widget currently shows its castable sprite -- i.e. we have the runes/level and no
	 *  thrall is already up. When the castable sprite is unknown (-1) the gate is disabled (always true) so an
	 *  un-mapped thrall still works off the timer + prayer gates alone. */
	public boolean castable(MirrorState mirror)
	{
		if (castableSpriteId <= 0) return true;
		return mirror.getWidgetSpriteId(castWidgetId) == castableSpriteId;
	}

	/** Max lifespan in TICKS for a thrall cast right now: 0.6 x magic level seconds / 0.6s per tick == magic level. */
	public static int durationTicksFor(MirrorState mirror)
	{
		return Math.max(1, mirror.getMagicLevel());
	}

	/** True once the previous thrall has (at latest) expired, so another can be summoned. */
	public boolean expired(int tick)
	{
		return !everCast || tick - lastCastTick >= castDurationTicks;
	}

	/**
	 * True when a recast is DUE and possible. The DURATION TIMER ({@link #expired}) is the gate for 'is the
	 * current thrall gone' -- the thrall spells do NOT grey while a thrall is active, so the lit sprite can't
	 * tell us that; only the timer (0.6 x magic level seconds == magic-level ticks, latched at cast) can. The
	 * rest are cast REQUIREMENTS: {@value #PRAYER_COST} prayer, the Book of the Dead in the inventory, the
	 * Arceuus book open, and the spell lit ({@link #castable} -- runes + magic level present). The boss layers
	 * its own region/phase conditions on top before calling {@link #maintain}.
	 */
	public boolean ready(int tick, MirrorState mirror)
	{
		return expired(tick)
			&& mirror.getPrayer() >= PRAYER_COST
			&& mirror.countItem(REQUIRED_ITEM_ID) > 0
			&& onArceuus(mirror)
			&& castable(mirror);
	}

	/** Fire the cast and latch tick + duration. Caller must have gated on {@link #ready} and boss conditions. */
	public void cast(ClickRouter router, MirrorState mirror, int tick)
	{
		router.castWidget(castWidgetId, "Cast", thrallName);
		lastCastTick = tick;
		castDurationTicks = durationTicksFor(mirror);
		everCast = true;
	}

	/**
	 * Cast NOW iff it is {@link #ready} (due + affordable) and the boss says conditions are ok. ASAP policy:
	 * no free-tick weave gate here -- the one-tick cost is accepted. Returns true iff it cast this tick.
	 */
	public boolean maintain(int tick, MirrorState mirror, ClickRouter router, boolean conditionsOk)
	{
		if (!conditionsOk || !ready(tick, mirror)) return false;
		cast(router, mirror, tick);
		return true;
	}

	/** Forget the current thrall (fight start/end, or on leaving the arena) so the next valid tick recasts. */
	public void reset()
	{
		everCast = false;
		lastCastTick = Integer.MIN_VALUE / 2;
		castDurationTicks = 1;
	}

}
