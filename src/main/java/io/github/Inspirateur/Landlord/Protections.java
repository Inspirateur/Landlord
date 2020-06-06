package io.github.Inspirateur.Landlord;

public enum Protections {
	mobGrief(new Price(.001, Currencies.gold)),
	playerGrief(new Price(.004, Currencies.iron)),
	PVP(new Price(.001, Currencies.diamond));

	public final Price price;

	Protections(Price price) {
		this.price = price;
	}
}
