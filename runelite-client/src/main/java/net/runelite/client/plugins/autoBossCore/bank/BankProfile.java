package net.runelite.client.plugins.autoBossCore.bank;

/**
 * Per-boss banking config for {@link BankRun}. Scope is the POH + Nexus navigation: how to teleport to the
 * POH, whether to top up at the rejuvenation pool, and which Nexus destination to take to the bank. The actual
 * withdraw/deposit at the destination bank is the boss's own job (BankRun stops once the bank is open).
 */
public final class BankProfile
{
	public enum TeleportKind { TELETAB, HOUSE_TELE_SPELL }

	/** How we get to the POH. */
	public final TeleportKind teleportKind;
	/** TELETAB: the house-teleport tablet item id, and the inventory option to use it (usually "Break"). */
	public final int teleTabItemId;
	public final String teleTabOption;
	/** HOUSE_TELE_SPELL: the spellbook widget (CC_OP param1), its option ("Cast") and coloured target text. */
	public final int houseTeleWidgetId;
	public final String houseTeleOption;
	public final String houseTeleName;
	/** The Portal Nexus option that teleports to the bank (e.g. "Lunar Isle"). */
	public final String nexusOption;
	/** Drink from the rejuvenation pool (restore stats + run) on the way through the POH. */
	public final boolean restoreAtPool;

	private BankProfile(Builder b)
	{
		this.teleportKind     = b.teleportKind;
		this.teleTabItemId    = b.teleTabItemId;
		this.teleTabOption    = b.teleTabOption;
		this.houseTeleWidgetId= b.houseTeleWidgetId;
		this.houseTeleOption  = b.houseTeleOption;
		this.houseTeleName    = b.houseTeleName;
		this.nexusOption      = b.nexusOption;
		this.restoreAtPool    = b.restoreAtPool;
	}

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private TeleportKind teleportKind = TeleportKind.HOUSE_TELE_SPELL;
		private int teleTabItemId = -1;
		private String teleTabOption = "Break";
		private int houseTeleWidgetId = -1;
		private String houseTeleOption = "Cast";
		private String houseTeleName = "";
		private String nexusOption = "Lunar Isle";
		private boolean restoreAtPool = true;

		/** Teleport to the POH by breaking a house-teleport tablet. */
		public Builder teletab(int itemId, String option)
		{
			this.teleportKind = TeleportKind.TELETAB; this.teleTabItemId = itemId; this.teleTabOption = option; return this;
		}

		/** Teleport to the POH by casting the house-teleport spell (CC_OP widget, e.g. 14286882). */
		public Builder houseTeleSpell(int widgetId, String option, String targetName)
		{
			this.teleportKind = TeleportKind.HOUSE_TELE_SPELL; this.houseTeleWidgetId = widgetId;
			this.houseTeleOption = option; this.houseTeleName = targetName; return this;
		}

		public Builder nexusOption(String o)      { this.nexusOption = o; return this; }
		public Builder restoreAtPool(boolean v)   { this.restoreAtPool = v; return this; }
		public BankProfile build()                { return new BankProfile(this); }
	}
}
