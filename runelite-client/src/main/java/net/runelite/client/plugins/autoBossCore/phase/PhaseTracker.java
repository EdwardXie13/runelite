package net.runelite.client.plugins.autoBossCore.phase;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Generic predicate-driven phase enum machine.
 * <p>
 * The boss strategy declares its phase enum, registers (phase -> predicate) transitions,
 * then calls {@link #update()} every tick. The tracker records the current phase and the
 * tick/time it was entered so overlays can show elapsed time in phase and the strategy
 * can time-gate decisions.
 * <p>
 * Only the FIRST matching transition fires per tick, so register in priority order
 * (most specific first).
 */
@Slf4j
public class PhaseTracker<E extends Enum<E>>
{
	private final List<Transition<E>> transitions = new ArrayList<>();
	private final E initial;

	private E current;
	private int currentSinceTick = 0;

	public PhaseTracker(E initial)
	{
		this.initial = initial;
		this.current = initial;
	}

	public static final class Transition<E extends Enum<E>>
	{
		public final E to;
		public final BooleanSupplier when;
		public final String label;

		public Transition(E to, BooleanSupplier when, String label)
		{
			this.to = to;
			this.when = when;
			this.label = label;
		}
	}

	/** Register a transition. {@code when} is evaluated every call to {@link #update(int)}. */
	public PhaseTracker<E> on(E to, BooleanSupplier when, String label)
	{
		transitions.add(new Transition<>(to, when, label));
		return this;
	}

	public E current()         { return current; }
	public int currentSince()  { return currentSinceTick; }
	public int ticksInPhase(int tickCounter) { return tickCounter - currentSinceTick; }

	/** Evaluate transitions. Returns the (possibly unchanged) new phase. */
	public E update(int tickCounter)
	{
		for (Transition<E> t : transitions)
		{
			if (t.to == current) continue;
			try
			{
				if (t.when.getAsBoolean())
				{
					log.debug("[phase] {} -> {} ({}) at tick {}",
						current, t.to, t.label, tickCounter);
					current = t.to;
					currentSinceTick = tickCounter;
					break;
				}
			}
			catch (Throwable th)
			{
				log.debug("[phase] transition predicate '{}' threw: {}", t.label, th.toString());
			}
		}
		return current;
	}

	public void reset(int tickCounter)
	{
		current = initial;
		currentSinceTick = tickCounter;
	}
}
