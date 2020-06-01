package io.github.Inspirateur.Landlord;

enum Currencies {
	iron("iron_ingot", "iron_block"),
	gold("gold_ingot", "gold_block"),
	emerald("emerald", "emerald_block"),
	diamond("diamond", "diamond_block");

	// An <blockID> is worth <multiplier> <itemID>
	public final String itemID;
	public final String blockID;
	public final int multiplier;

	Currencies(String itemID, String blockID) {
		this(itemID, blockID, 9);
	}

	Currencies(String itemID, String blockID, int multiplier) {
		this.itemID = itemID;
		this.blockID = blockID;
		this.multiplier = multiplier;
	}

	public String toString(double amount) {
		StringBuilder msg = new StringBuilder();
		msg.append(String.format("%d %s", (int)amount, this.toString()));
		if (amount < multiplier) {
			return msg.toString();
		}
		int blockAmount = (int)amount/multiplier;
		int itemAmount = (int)amount-blockAmount*multiplier;
		msg.append(String.format(
			" or %d %s %d %s", blockAmount, blockID.replace('_', ' '),
			itemAmount, this.toString()
		));
		return msg.toString();
	}
}
