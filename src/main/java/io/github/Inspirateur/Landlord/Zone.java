package io.github.Inspirateur.Landlord;

import java.io.Serializable;
import java.util.*;

public class Zone implements Serializable {
	public Point pMin;
	public Point pMax;
	public UUID owner;
	public Set<UUID> guests;
	public Map<Protections, Boolean> protecs;

	public Zone(PartialZone zone) {
		this(zone.corner1, zone.corner2);
	}

	public Zone(Point p1, Point p2) {
		this.pMin = Point.min(p1, p2);
		this.pMax = Point.max(p1, p2);
		this.protecs = new HashMap<>();
		for(Protections p: Protections.values()) {
			this.protecs.put(p, false);
		}
		this.guests = new HashSet<>();
	}

	public boolean hasRights(UUID playerID) {
		return playerID.equals(this.owner) || guests.contains(playerID);
	}

	public int getVolume() {
		return (this.pMax.x-this.pMin.x)*(this.pMax.y-this.pMin.y)*(this.pMax.z-this.pMin.z);
	}

	public boolean contains(Point p) {
		return Point.leq(pMin, p) && Point.geq(pMax, p);
	}

	public boolean overlaps(Zone zone) {
		return this.contains(zone.pMin) || zone.contains(this.pMin);
	}

	public boolean overlaps(PartialZone zone) {
		return this.overlaps(new Zone(zone.corner1, zone.corner2));
	}
}
