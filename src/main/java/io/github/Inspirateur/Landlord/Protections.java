package io.github.Inspirateur.Landlord;

public enum Protections {
	mobGrief(new Price(0.008, Currencies.gold)),
	playerGrief(new Price(.008, Currencies.iron)),
	PVP(new Price(.005, Currencies.diamond));

	public final Price price;

	Protections(Price price) {
		this.price = price;
	}
}
