package io.github.Inspirateur.Landlord;

import java.io.*;
import java.util.*;

public class LandData {
	// <world, <player, [zone1, ...]>>
	private Map<UUID, Map<UUID , List<Zone>>> zones;

	public LandData() {
		this.load();
	}

	public Optional<Zone> getZone(UUID w, Point p) {
		for (List<Zone> zones: this.zones.get(w).values()) {
			for (Zone zone: zones) {
				if(zone.contains(p)) {
					return Optional.of(zone);
				}
			}
		}
		return Optional.empty();
	}

	public void addZone(UUID world, Zone zone) {
		if(!zones.containsKey(world)) {
			zones.put(world, new HashMap<>());
		}
		if(!zones.get(world).containsKey(zone.owner)) {
			zones.get(world).put(zone.owner, new ArrayList<>());
		}
		zones.get(world).get(zone.owner).add(zone);
		this.save();
	}

	private void save() {
		try {
			FileOutputStream fileOut = new FileOutputStream("plugins/Landlord/land_data.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this.zones);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void load() {
		this.zones = new HashMap<>();
		try {
			FileInputStream fileIn = new FileInputStream("plugins/Landlord/land_data.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			//noinspection unchecked
			this.zones = (Map<UUID, Map<UUID, List<Zone>>>) in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException | ClassNotFoundException i) {
			i.printStackTrace();
			this.save();
		}
	}
}
