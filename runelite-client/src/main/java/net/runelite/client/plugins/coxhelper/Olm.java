package net.runelite.client.plugins.coxhelper;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
@Getter
@Setter
public class Olm
{
	public static final int HEAD_GAMEOBJECT_RISING = 29880;
	public static final int HEAD_GAMEOBJECT_READY = 29881;
	public static final int LEFT_HAND_GAMEOBJECT_RISING = 29883;
	public static final int LEFT_HAND_GAMEOBJECT_READY = 29884;
	public static final int RIGHT_HAND_GAMEOBJECT_RISING = 29886;
	public static final int RIGHT_HAND_GAMEOBJECT_READY = 29887;

	// Metronome tiles. Indices: 0 = E, 1 = D, 2 = C, 3 = A. Ordered so tileIndex advances
	// through the 16-tick cycle in the sequence E (ticks 15,0,1,2) -> D -> C -> A.
	// Coords are template world points inside Olm chamber region 12889; WorldPoint.toLocalInstance
	// converts them to the current instance at read time.
	private static final WorldPoint[] WEST_METRONOME_TILES = {
		new WorldPoint(3229, 5747, 0), // E
		new WorldPoint(3229, 5745, 0), // D
		new WorldPoint(3229, 5743, 0), // C
		new WorldPoint(3229, 5741, 0), // B
		new WorldPoint(3229, 5739, 0), // A
	};

	private static final WorldPoint[] EAST_METRONOME_TILES = {
		new WorldPoint(3236, 5733, 0), // E
		new WorldPoint(3236, 5735, 0), // D
		new WorldPoint(3236, 5737, 0), // C
		new WorldPoint(3236, 5739, 0), // B
		new WorldPoint(3236, 5741, 0), // A
	};

	// cycleTick -> index into the METRONOME_TILES arrays (E=0, D=1, C=2, B=3, A=4).
	// Same pattern for both orientations; only the WorldPoints differ.
	//   0-1 E | 2-5 D | 6-9 C | 10 B | 11 A | 12 B | 13 C | 14 D | 15 E
	private static final int[] TILE_INDEX_BY_TICK = {
		0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 4, 3, 2, 1, 0
	};

	public static final int OLM_BURN = 1351;
	public static final int OLM_LIGHTNING = 1356;
	public static final int OLM_TELEPORT = 1359;
	public static final int OLM_HEAL = 1363;
	public static final int OLM_CRYSTAL = 1447;

	private final Client client;
	private final CoxPlugin plugin;
	private final CoxConfig config;

	private final List<WorldPoint> healPools = new ArrayList<>();
	private final List<WorldPoint> portals = new ArrayList<>();
	private final Set<Victim> victims = new HashSet<>();
	private int portalTicks = 10;

	private boolean active = false; // in fight
	private boolean firstPhase = false;
	private boolean finalPhase = false;
	private PhaseType phaseType = PhaseType.UNKNOWN;

	private GameObject hand = null;
	private OlmAnimation handAnimation = OlmAnimation.UNKNOWN;
	private GameObject head = null;
	private OlmAnimation headAnimation = OlmAnimation.UNKNOWN;

	private int ticksUntilNextAttack = -1;
	private int attackCycle = 1;
	private int specialCycle = 1;

	private boolean crippled = false;
	private int crippleTicks = 45;

	private Prayer prayer = null;
	private long lastPrayTime = 0;

	@Inject
	private Olm(final Client client, final CoxPlugin plugin, final CoxConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	public void startPhase()
	{
		this.firstPhase = !this.active;
		this.active = true;
		this.ticksUntilNextAttack = -1;
		this.attackCycle = 1;
		this.specialCycle = 1;
		this.crippled = false;
		this.crippleTicks = 45;
		this.prayer = null;
		this.lastPrayTime = 0;
		this.headAnimation = OlmAnimation.UNKNOWN;
		this.handAnimation = OlmAnimation.UNKNOWN;
	}

	public void hardRest()
	{
		this.active = false;
		this.firstPhase = false;
		this.finalPhase = false;
		this.phaseType = PhaseType.UNKNOWN;
		this.hand = null;
		this.head = null;
		this.headAnimation = OlmAnimation.UNKNOWN;
		this.handAnimation = OlmAnimation.UNKNOWN;
		this.ticksUntilNextAttack = -1;
		this.attackCycle = 1;
		this.specialCycle = 1;
		this.healPools.clear();
		this.portals.clear();
		this.portalTicks = 10;
		this.victims.clear();
		this.crippled = false;
		this.crippleTicks = 45;
		this.prayer = null;
		this.lastPrayTime = 0;
	}

	void setPrayer(Prayer pray)
	{
		this.prayer = pray;
		this.lastPrayTime = System.currentTimeMillis();
	}

	void cripple()
	{
		this.crippled = true;
		this.crippleTicks = 45;
	}

	void uncripple()
	{
		this.crippled = false;
		this.crippleTicks = 45;
	}

	public void update()
	{
		this.updateVictims();
		this.updateCrippleSticks();
		this.updateSpecials();
		this.incrementTickCycle();
		this.headAnimations();
		this.handAnimations();
	}

	public void incrementTickCycle()
	{
		if (this.ticksUntilNextAttack == 0) {
			// highlight red metronome
		}
		else if (this.ticksUntilNextAttack == 1)
		{
			this.ticksUntilNextAttack = 4;
			this.incrementAttackCycle();
		}
		else if (this.ticksUntilNextAttack != -1)
		{
			this.ticksUntilNextAttack--;
		}

	}

	public void incrementAttackCycle()
	{
		if (this.attackCycle == 4)
		{
			this.attackCycle = 1;
			this.incrementSpecialCycle();
		}
		else
		{
			this.attackCycle++;
		}
	}

	public void incrementSpecialCycle()
	{
		if ((this.specialCycle == 3 && !this.finalPhase) || this.specialCycle == 4)
		{
			this.specialCycle = 1;
		}
		else
		{
			this.specialCycle++;
		}
	}

	public void specialSync(OlmAnimation currentAnimation)
	{
		this.ticksUntilNextAttack = 4;
		this.attackCycle = 1;
		switch (currentAnimation)
		{
			case LEFT_HAND_CRYSTALS1:
			case LEFT_HAND_CRYSTALS2:
				this.specialCycle = 2;
				break;
			case LEFT_HAND_LIGHTNING1:
			case LEFT_HAND_LIGHTNING2:
				this.specialCycle = 3;
				break;
			case LEFT_HAND_PORTALS1:
			case LEFT_HAND_PORTALS2:
				this.specialCycle = this.finalPhase ? 4 : 1;
				break;
			case LEFT_HAND_HEAL1:
			case LEFT_HAND_HEAL2:
				this.specialCycle = 1;
				break;
		}
	}

	void updateCrippleSticks()
	{
		if (!this.crippled)
		{
			return;
		}

		this.crippleTicks--;
		if (this.crippleTicks <= 0)
		{
			this.crippled = false;
			this.crippleTicks = 45;
		}
	}

	void updateVictims()
	{
		if (this.victims.size() > 0)
		{
			this.victims.forEach(Victim::updateTicks);
			this.victims.removeIf(victim -> victim.getTicks() <= 0);
		}
	}

	void updateSpecials()
	{
		this.healPools.clear();
		this.portals.clear();
		this.client.clearHintArrow();

		for (GraphicsObject o : this.client.getGraphicsObjects())
		{
			if (o.getId() == OLM_TELEPORT)
			{
				this.portals.add(WorldPoint.fromLocal(this.client, o.getLocation()));
			}
			if (o.getId() == OLM_HEAL)
			{
				this.healPools.add(WorldPoint.fromLocal(this.client, o.getLocation()));
			}
			if (!this.portals.isEmpty())
			{
				this.portalTicks--;
				if (this.portalTicks <= 0)
				{
					this.client.clearHintArrow();
					this.portalTicks = 10;
				}
			}
		}
	}

	/**
	 * Position within Olm's 16-tick attack cycle (0–15), or -1 if state is not yet initialized.
	 * cycleTick = (attackCycle - 1) * 4 + (4 - ticksUntilNextAttack)
	 * Layout: 0=post-attack empty, 3=Nauto attack tick, 7=Null slot end,
	 * 11=Sauto attack tick, 15=Special attack tick.
	 */
	public int getCycleTick()
	{
		if (this.attackCycle < 1 || this.attackCycle > 4
			|| this.ticksUntilNextAttack < 1 || this.ticksUntilNextAttack > 4)
		{
			return -1;
		}
		return (this.attackCycle - 1) * 4 + (4 - this.ticksUntilNextAttack);
	}

	/**
	 * Current metronome tile based on cycleTick and Olm's orientation.
	 * Cycle mapping (cycleTick -> tile / color):
	 *   ticks 15,0,1,2 -> E (red at 15, then yellow, green, blue)
	 *   ticks 3-6     -> D (red at 3,  then yellow, green, blue)
	 *   ticks 7-10    -> C (red at 7,  then yellow, green, blue)
	 *   ticks 11-14   -> A (red at 11, then yellow, green, blue)
	 * Returns null if outside a valid cycle or before Olm is positioned.
	 */
	public WorldPoint getMetronomeTile()
	{
		int tick = this.getCycleTick();
		if (tick < 0 || this.hand == null || this.head == null)
		{
			return null;
		}
		boolean olmOnWest = this.hand.getWorldLocation().getY() > this.head.getWorldLocation().getY();
		WorldPoint[] tiles = olmOnWest ? WEST_METRONOME_TILES : EAST_METRONOME_TILES;
		int tileIndex = TILE_INDEX_BY_TICK[tick];
		WorldPoint template = tiles[tileIndex];
		java.util.Collection<WorldPoint> instances = WorldPoint.toLocalInstance(this.client, template);
		if (instances == null || instances.isEmpty())
		{
			return null;
		}
		return instances.iterator().next();
	}

	/**
	 * Color index for the current metronome tick: 1=red, 2=yellow, 3=green, 4=blue.
	 * Returns 0 if outside a valid cycle. Red always coincides with the tile changing.
	 */
	public int getMetronomeColorIndex()
	{
		int tick = this.getCycleTick();
		if (tick < 0)
		{
			return 0;
		}
		int shifted = (tick + 1) % 16;
		return (shifted % 4) + 1;
	}

	private void headAnimations()
	{
		if (this.head == null)
		{
			return;
		}

		OlmAnimation currentAnimation = OlmAnimation.fromId(((DynamicObject) this.head.getRenderable()).getAnimation().getId());

		if (currentAnimation == this.headAnimation)
		{
			return;
		}

		switch (currentAnimation)
		{
			case HEAD_RISING_2:
			case HEAD_ENRAGED_RISING_2:
				this.ticksUntilNextAttack = this.firstPhase ? 4 : 8;
				this.attackCycle = 1;
				this.specialCycle = 1;
				break;
			case HEAD_ENRAGED_LEFT:
			case HEAD_ENRAGED_MIDDLE:
			case HEAD_ENRAGED_RIGHT:
				this.finalPhase = true;
				break;

			// heading towards left (melee hand)
			case HEAD_MIDDLE_TO_LEFT:
			case HEAD_ENRAGED_MIDDLE_TO_LEFT:
			case HEAD_RIGHT_TO_MIDDLE:
			case HEAD_ENRAGED_RIGHT_TO_MIDDLE:
			case HEAD_RIGHT_TO_LEFT:
			case HEAD_ENRAGED_RIGHT_TO_LEFT:
				break;

			// heading towards right (mage hand)
			case HEAD_MIDDLE_TO_RIGHT:
			case HEAD_ENRAGED_MIDDLE_TO_RIGHT:
			case HEAD_LEFT_TO_MIDDLE:
			case HEAD_ENRAGED_LEFT_TO_MIDDLE:
			case HEAD_LEFT_TO_RIGHT:
			case HEAD_ENRAGED_LEFT_TO_RIGHT:
				break;

//			case HEAD_AUTO_LEFT:
//			case HEAD_ENRAGED_AUTO_LEFT:
//				if (this.hand != null)
//				{
//					// 3x3 box centered on the visual center of Olm's hand.
//					// GameObject.getWorldLocation() returns the SW tile; the hand is a 3x3 object,
//					// so its center tile is (SW.x + 1, SW.y + 1).
//					WorldPoint sw = this.hand.getWorldLocation();
//					WorldPoint center = new WorldPoint(sw.getX() + 1, sw.getY() + 1, sw.getPlane());
//					for (int dx = -1; dx <= 1; dx++)
//					{
//						for (int dy = -1; dy <= 1; dy++)
//						{
//							this.headAutoLeftTiles.add(new WorldPoint(center.getX() + dx, center.getY() + dy, center.getPlane()));
//						}
//					}
//				}
//				break;
		}

		this.headAnimation = currentAnimation;
	}

	private WorldPoint templateRegionToInstance(int regionId, int regionX, int regionY, int plane)
	{
		WorldPoint template = WorldPoint.fromRegion(regionId, regionX, regionY, plane);
		java.util.Collection<WorldPoint> instances = WorldPoint.toLocalInstance(this.client, template);
		if (instances == null || instances.isEmpty())
		{
			return null;
		}
		return instances.iterator().next();
	}

	private void handAnimations()
	{
		if (this.hand == null)
		{
			System.out.println("the hand is null in handAnimations");
			return;
		}

		OlmAnimation currentAnimation = OlmAnimation.fromId(((DynamicObject) this.hand.getRenderable()).getAnimation().getId());

		if (currentAnimation == this.handAnimation)
		{
			return;
		}

		switch (currentAnimation)
		{
			case LEFT_HAND_CRYSTALS1:
			case LEFT_HAND_CRYSTALS2:
			case LEFT_HAND_LIGHTNING1:
			case LEFT_HAND_LIGHTNING2:
			case LEFT_HAND_PORTALS1:
			case LEFT_HAND_PORTALS2:
			case LEFT_HAND_HEAL1:
			case LEFT_HAND_HEAL2:
				this.specialSync(currentAnimation);
				break;
			case LEFT_HAND_CRIPPLING:
				this.cripple();
				break;
			case LEFT_HAND_UNCRIPPLING1:
			case LEFT_HAND_UNCRIPPLING2:
				this.uncripple();
				break;
		}

		this.handAnimation = currentAnimation;
	}

	public enum PhaseType
	{
		FLAME,
		ACID,
		CRYSTAL,
		UNKNOWN,
	}
}
