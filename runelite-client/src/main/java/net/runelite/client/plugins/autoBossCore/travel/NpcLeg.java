package net.runelite.client.plugins.autoBossCore.travel;

import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Interact with an NPC via a menu option. Human model: while the NPC is NOT in the scene we only walk the
 * approach hint (to pull the scene to it); the instant it spawns we latch the tick and wait a per-NPC random
 * beat (so we don't click the frame it loads) while still closing ground, then fire the option -- the server
 * paths the last stretch, so we usually interact mid-approach. Completion is the caller's {@link DoneSignal}
 * (a boat "Travel" finishes on a region change; a banker on the bank opening; a chat NPC on whatever it sets).
 */
final class NpcLeg implements TravelLeg
{
	private static final int WALK_DEBOUNCE = 4;
	private static final int ACT_DEBOUNCE  = 3;

	private final int npcId;
	private final MenuAction action;
	private final String option;
	private final WorldPoint approachHint;
	private final int jitterMin, jitterMax;
	private final String[] chatPath;     // null = this interaction opens no dialogue
	private final DoneSignal signal;

	NpcLeg(int npcId, MenuAction action, String option, WorldPoint approachHint,
	       int jitterMin, int jitterMax, String[] chatPath, DoneSignal signal)
	{
		this.npcId = npcId; this.action = action; this.option = option; this.approachHint = approachHint;
		this.jitterMin = jitterMin; this.jitterMax = jitterMax; this.chatPath = chatPath; this.signal = signal;
	}

	public boolean done(TravelContext ctx, LegScratch s) { return signal.met(ctx); }

	public void act(TravelContext ctx, int tick, LegScratch s)
	{
		// Opportunistic: if this interaction opens a dialogue AND we were given a path, click through it.
		if (chatPath != null && advanceChatIfOpen(ctx, tick)) return;

		NPC npc = ctx.npc(npcId);
		if (npc == null)                                        // not loaded -> walk the hint to load it
		{
			s.clearLatch();
			walkHint(ctx, tick);
			return;
		}
		if (!s.latched()) s.latch(tick, rollJitter());          // just spawned -> start the human beat
		if (tick - s.latchTick() < s.jitter())                  // inside the beat -> keep closing / idle
		{
			walkHint(ctx, tick);
			return;
		}
		if (ctx.actionReady(tick, ACT_DEBOUNCE))                // beat elapsed -> interact
		{
			ctx.router.interactNpc(npc, action, option);
			ctx.markAction(tick);
		}
	}

	private void walkHint(TravelContext ctx, int tick)
	{
		if (approachHint != null && ctx.actionReady(tick, WALK_DEBOUNCE))
		{
			ctx.router.walkTo(approachHint);
			ctx.markAction(tick);
		}
	}

	private int rollJitter()
	{
		return jitterMax <= jitterMin ? Math.max(0, jitterMin)
			: ThreadLocalRandom.current().nextInt(jitterMin, jitterMax + 1);
	}

	// Placeholder: multi-step dialogue automation lands with a DialogueReader in the state package. Until
	// then, NPC interactions that open a chat box must be finished by their DoneSignal; this is a no-op.
	private boolean advanceChatIfOpen(TravelContext ctx, int tick) { return false; }

	public int[] regions() { return approachHint != null ? new int[]{ approachHint.getRegionID() } : new int[0]; }
	public String label()  { return "travel: " + option + " npc " + npcId; }
}
