package io.github.Inspirateur.Landlord;

public class NotInOwnZoneException extends RuntimeException {
	public NotInOwnZoneException() {
		super("The player is not building a zone and is not standing in a zone he owns");
	}
}
