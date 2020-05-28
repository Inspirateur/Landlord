package io.github.Inspirateur.Landlord;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LandData {
	private Map<String, Map<String , ArrayList<Zone>>> zones;

	public LandData() {
		this.load();
	}

	public void addZone(String world, String player, Zone zone) {
		if(!zones.containsKey(world)) {
			zones.put(world, new HashMap<>());
		}
		if(!zones.get(world).containsKey(player)) {
			zones.get(world).put(player, new ArrayList<>());
		}
		zones.get(world).get(player).add(zone);
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
			this.zones = (Map<String, Map<String, ArrayList<Zone>>>) in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException | ClassNotFoundException i) {
			i.printStackTrace();
			this.save();
		}
	}
}
