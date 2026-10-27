package net.runelite.client.plugins.autoBossCore.travel;

import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * A declarative, reusable journey: an ordered list of {@link TravelLeg}s built once and held as a
 * {@code static final} constant. Convention: the FIRST leg should be a from-anywhere anchor (a house /
 * home teleport) so the runner's "lost -> restart at leg 0" recovery can always re-establish position.
 */
public final class TravelRoute
{
	private final String name;
	private final List<TravelLeg> legs;

	private TravelRoute(String name, List<TravelLeg> legs) { this.name = name; this.legs = legs; }

	public String name()        { return name; }
	public int size()           { return legs.size(); }
	public TravelLeg leg(int i) { return legs.get(i); }

	public static Builder builder(String name) { return new Builder(name); }

	public static final class Builder
	{
		private final String name;
		private final List<TravelLeg> legs = new ArrayList<>();

		private Builder(String name) { this.name = name; }

		/** Anchor / region teleport by casting a spell widget (done when {@code doneRegion} loads). */
		public Builder teleportSpell(int widgetId, String option, String target, int doneRegion)
		{ legs.add(TeleportLeg.spell(widgetId, option, target, doneRegion)); return this; }

		/** Anchor / region teleport by breaking a tablet (done when {@code doneRegion} loads). */
		public Builder teleportTab(int itemId, String option, int doneRegion)
		{ legs.add(TeleportLeg.tab(itemId, option, doneRegion)); return this; }

		/** Interact with any object of {@code ids} once it is in the scene; walk {@code approachHint} until then. */
		public Builder object(int[] ids, MenuAction action, String option, WorldPoint approachHint,
		                      int jitterMin, int jitterMax, DoneSignal signal)
		{ legs.add(new ObjectLeg(ids, null, action, option, approachHint, jitterMin, jitterMax, signal)); return this; }

		/** Interact with the SPECIFIC object of {@code ids} on {@code tile}; the hint defaults to that tile. */
		public Builder objectAtTile(int[] ids, WorldPoint tile, MenuAction action, String option,
		                           int jitterMin, int jitterMax, DoneSignal signal)
		{ legs.add(new ObjectLeg(ids, tile, action, option, tile, jitterMin, jitterMax, signal)); return this; }

		/** Interact with an NPC once it spawns in the scene; walk {@code approachHint} until then. */
		public Builder npc(int npcId, MenuAction action, String option, WorldPoint approachHint,
		                  int jitterMin, int jitterMax, String[] chatPath, DoneSignal signal)
		{ legs.add(new NpcLeg(npcId, action, option, approachHint, jitterMin, jitterMax, chatPath, signal)); return this; }

		/** Pure positioning: walk to a tile, finish when standing on it. */
		public Builder walk(WorldPoint tile) { legs.add(new WalkLeg(tile)); return this; }

		/** Hold until a signal, no action. */
		public Builder gate(DoneSignal signal, int... regions) { legs.add(new GateLeg(signal, regions)); return this; }

		/** Escape hatch for a bespoke leg. */
		public Builder leg(TravelLeg custom) { legs.add(custom); return this; }

		public TravelRoute build() { return new TravelRoute(name, new ArrayList<>(legs)); }
	}
}
