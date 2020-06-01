package io.github.Inspirateur.Landlord;

public enum Protections {
	mobGrief(new Price(0.006, Currencies.gold)),
	playerGrief(new Price(.006, Currencies.iron)),
	PVP(new Price(.002, Currencies.diamond));

	public final Price price;

	Protections(Price price) {
		this.price = price;
	}
}
