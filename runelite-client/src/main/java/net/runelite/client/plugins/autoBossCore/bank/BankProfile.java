package net.runelite.client.plugins.autoBossCore.bank;

import net.runelite.api.coords.WorldPoint;

/**
 * Per-boss banking config for {@link BankRun}. Scope is POH -> Lunar Isle bank only: whether to top up at the
 * rejuvenation pool, which Nexus destination to take to the bank, and which booth to open. Getting to the POH
 * (travel) and the withdraw/deposit at the bank are the boss's own job -- BankRun stops once the bank is open.
 */
public final class BankProfile
{
	/** The Portal Nexus option that teleports to the bank (e.g. "Lunar Isle"). */
	public final String nexusOption;
	/** Drink from the rejuvenation pool (restore stats + run) on the way through the POH. */
	public final boolean restoreAtPool;
	/** Exact WORLD tile of the specific bank booth to open; null = open the first booth found by id. */
	public final WorldPoint bankBoothTile;

	private BankProfile(Builder b)
	{
		this.nexusOption   = b.nexusOption;
		this.restoreAtPool = b.restoreAtPool;
		this.bankBoothTile = b.bankBoothTile;
	}

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private String nexusOption = "Lunar Isle";
		private boolean restoreAtPool = true;
		private WorldPoint bankBoothTile = null;

		public Builder nexusOption(String o)      { this.nexusOption = o; return this; }
		public Builder bankBoothTile(WorldPoint t){ this.bankBoothTile = t; return this; }
		public Builder restoreAtPool(boolean v)   { this.restoreAtPool = v; return this; }
		public BankProfile build()                { return new BankProfile(this); }
	}
}
