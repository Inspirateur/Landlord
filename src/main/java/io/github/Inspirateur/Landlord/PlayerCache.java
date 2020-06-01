package io.github.Inspirateur.Landlord;

import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerCache {
	public Map<UUID, String> playerNames;
	public Map<String, UUID> playerUID;

	public PlayerCache() {
		this.load();
	}

	public void update(Player player) {
		String pName = player.getName();
		UUID pUID = player.getUniqueId();
		playerNames.put(pUID, pName);
		playerUID.put(pName, pUID);
	}

	public String get(UUID pUID) {
		return playerNames.get(pUID);
	}

	public UUID get(String pName) {
		return playerUID.get(pName);
	}

	public boolean contains(String pName) {
		return playerUID.containsKey(pName);
	}

	public boolean contains(UUID pUID) {
		return playerNames.containsKey(pUID);
	}

	private void save() {
		try {
			FileOutputStream fileOut = new FileOutputStream("plugins/Landlord/player_names.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this.playerNames);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileOutputStream fileOut = new FileOutputStream("plugins/Landlord/player_uid.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this.playerUID);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void load() {
		this.playerNames = new HashMap<>();
		this.playerUID = new HashMap<>();
		if(new File("plugins/Landlord/player_names.ser").isFile()) {
			try {
				FileInputStream fileIn = new FileInputStream("plugins/Landlord/player_names.ser");
				ObjectInputStream in = new ObjectInputStream(fileIn);
				//noinspection unchecked
				this.playerNames = (Map<UUID, String>) in.readObject();
				in.close();
				fileIn.close();
			} catch (IOException | ClassNotFoundException i) {
				i.printStackTrace();
				this.save();
			}
			try {
				FileInputStream fileIn = new FileInputStream("plugins/Landlord/player_uid.ser");
				ObjectInputStream in = new ObjectInputStream(fileIn);
				//noinspection unchecked
				this.playerUID = (Map<String, UUID>) in.readObject();
				in.close();
				fileIn.close();
			} catch (IOException | ClassNotFoundException i) {
				i.printStackTrace();
			}
		} else {
			this.save();
		}
	}
}
