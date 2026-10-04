package net.runelite.client.plugins.autoBossCore.trip;

import lombok.Getter;

/**
 * Where in a boss trip the strategy is. Deliberately coarse -- fine-grained state lives
 * on the boss's own phase enum via {@link
 * net.runelite.client.plugins.autoBossCore.phase.PhaseTracker}.
 */
public class TripState
{
	public enum Stage
	{
		IDLE,              // not running, in home / bank
		TOPPING_OFF,       // pre-fight: eat / drink / stam / sip
		TRAVELING,         // walking to boss / portal / teleport chain
		IN_FIGHT,          // actively killing the boss, full supplies
		PUSHING_KILL,      // mid-fight, supplies running low, trying to finish
		LOOTING,           // post-death, grabbing drops
		RETURNING          // trip home / to bank
	}

	@Getter private Stage stage = Stage.IDLE;
	@Getter private int stageSinceTick = 0;

	public void set(Stage next, int tickCounter)
	{
		if (next == stage) return;
		this.stage = next;
		this.stageSinceTick = tickCounter;
	}

	public boolean is(Stage s) { return stage == s; }

	public int ticksInStage(int tickCounter) { return tickCounter - stageSinceTick; }
}
