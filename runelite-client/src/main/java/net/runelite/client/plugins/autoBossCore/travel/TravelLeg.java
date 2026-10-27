package net.runelite.client.plugins.autoBossCore.travel;

/**
 * One hop of a {@link TravelRoute}. The engine never asks a leg to "finish" -- it asks whether the leg is
 * already {@link #done} (a check on LIVE game state, never "did I click it"), and otherwise lets it make a
 * little progress with {@link #act}. Because done() is state-based, a route is resumable and self-healing:
 * start anywhere along it and the runner picks up at the first unsatisfied leg.
 *
 * <p>{@link #regions()} lists the map region(s) this leg operates in; the runner uses it to RE-LOCATE the
 * player each tick if they get displaced (random-event teleport, death respawn, relog). Return an empty
 * array to opt a leg out of localisation (e.g. a pure same-region walk -- its neighbours localise it).
 */
public interface TravelLeg
{
	/** True when this leg's goal is already satisfied by the current game state. */
	boolean done(TravelContext ctx, LegScratch s);

	/** Issue one tick of progress (walk / cast / interact). Must be debounced via {@code ctx}. */
	void act(TravelContext ctx, int tick, LegScratch s);

	/** Map regions this leg lives in, for re-localisation. Empty = do not localise on this leg. */
	int[] regions();

	/** Short human label for the overlay. */
	String label();
}
