package io.github.Inspirateur.Landlord;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Plugin, Listener {
	private LandData landData;
	private Map<UUID, String> playerNames;

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		playerNames.put(player.getUniqueId(), player.getName());
		System.out.println("bite");
	}

	@Override
	public void onEnable() {
		getLogger().info("onEnable is called!");
		landData = new LandData();
		playerNames = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		switch (label) {
			case "corner1":
				corner1(sender);
				break;
			case "corner2":
				corner2(sender);
				break;
			case "land":
				land(sender);
				break;
			case "protect":
				protect(sender);
				break;
			case "unprotect":
				unprotect(sender);
				break;
			case "add":
				add(sender, args);
			case "remove":
				remove(sender, args);
		}
		return super.onCommand(sender, command, label, args);
	}

	public void corner1(CommandSender sender) {

	}

	public void corner2(CommandSender sender) {

	}

	public void land(CommandSender sender) {
		Player player = (Player) sender;
		Point point = new Point(
				player.getLocation().getBlockX(),
				player.getLocation().getBlockY(),
				player.getLocation().getBlockZ()
		);
		UUID w = player.getWorld().getUID();
		Optional<Zone> zoneOpt = this.landData.getZone(w, point);
		if (zoneOpt.isPresent()) {
			Zone zone = zoneOpt.get();
			StringBuilder msg = new StringBuilder();
			msg.append("Zone Owner: ").append(playerNames.get(zone.owner)).append("\n");
			if (zone.guests.size() > 0) {
				msg.append("Guests: ").append(
					zone.guests.stream().map(g->playerNames.get(g)).collect(Collectors.joining(", "))
				).append("\n");
			}
			msg.append("Protections: ").append(
				zone.protecs.entrySet().stream().filter(Map.Entry::getValue)
					.map(p->p.getKey().toString()).collect(Collectors.joining(", "))
			).append("\n");
			msg.append("Volume: ").append(zone.getVolume()).append("\n");
			player.sendMessage(msg.toString());
		} else {
			player.sendMessage("You're not in a claimed land.");
		}
	}

	public void protect(CommandSender sender) {
	}

	public void unprotect(CommandSender sender) {
	}

	public void add(CommandSender sender, String[] args) {
	}

	public void remove(CommandSender sender, String[] args) {
	}
}
