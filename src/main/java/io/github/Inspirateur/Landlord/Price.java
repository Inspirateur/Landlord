package io.github.Inspirateur.Landlord;

class Price {
	public final double amount;
	public final Currencies currency;

	public Price(double amount, Currencies currency) {
		this.amount = amount;
		this.currency = currency;
	}
}
