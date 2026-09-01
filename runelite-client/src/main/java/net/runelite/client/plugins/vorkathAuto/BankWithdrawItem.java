package net.runelite.client.plugins.vorkathAuto;

/**
 * Per-item info needed to build a bank withdraw menuAction call, except the ItemID
 * (which is the map key when used in a Map<Integer, BankWithdrawItem>).
 *
 * `identifier` is the widget op index for the desired option:
 *   Withdraw-1   = 1
 *   Withdraw-5   = 2
 *   Withdraw-10  = 3
 *   Withdraw-X   = 4
 *   Withdraw-All = 5
 *   Withdraw-All-but-1 = 6
 *
 * Verify against the current bank build via RuneLite dev tools -> Menu Entries.
 */
public class BankWithdrawItem
{
    public final int quantity;      // how many times to invoke the menuAction
    public final int identifier;    // op index for the widget op
    public final String option;     // "Withdraw-1", "Withdraw-5", ...
    public final String targetName; // "<col=ff9040>Blood rune</col>"

    public BankWithdrawItem(int quantity, int identifier, String option, String targetName)
    {
        this.quantity = quantity;
        this.identifier = identifier;
        this.option = option;
        this.targetName = targetName;
    }
}
