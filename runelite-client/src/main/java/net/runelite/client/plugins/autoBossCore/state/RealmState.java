package net.runelite.client.plugins.autoBossCore.state;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Varbit-driven two-realm state helper (shadow realm, parallel dimensions, etc).
 * <p>
 * Boss plugin configures:
 * <ul>
 *   <li>The varbit id that encodes the realm state.</li>
 *   <li>The value for each realm.</li>
 * </ul>
 * Then queries {@link #inRealm(int)} / {@link #currentRealm()} any tick.
 * <p>
 * We don't bake in Whisperer's specific varbit here -- capture tells us the id first.
 */
@Singleton
public class RealmState
{
	private final MirrorState mirror;

	private int varbitId = -1;
	private int normalValue = 0;
	private int shadowValue = 1;

	@Inject
	public RealmState(MirrorState mirror)
	{
		this.mirror = mirror;
	}

	/** Configure which varbit encodes the realm state. */
	public void configure(int varbitId, int normalValue, int shadowValue)
	{
		this.varbitId = varbitId;
		this.normalValue = normalValue;
		this.shadowValue = shadowValue;
	}

	public int currentRealm()
	{
		if (varbitId < 0) return normalValue;
		return mirror.getVarbit(varbitId, normalValue);
	}

	public boolean inNormal() { return currentRealm() == normalValue; }
	public boolean inShadow() { return currentRealm() == shadowValue; }

	public boolean inRealm(int value) { return currentRealm() == value; }
}
