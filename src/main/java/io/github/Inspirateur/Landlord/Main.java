package io.github.Inspirateur.Landlord;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Plugin, Listener {
	private LandData landData;
	// TODO: save and load this on a file
	private Map<UUID, String> playerNames;
	private Map<UUID, PartialZone> partialZones;

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		playerNames.put(player.getUniqueId(), player.getName());
	}

	@Override
	public void onEnable() {
		getLogger().info("onEnable is called!");
		landData = new LandData();
		for(World world: getServer().getWorlds()) {
			landData.registerWorld(world.getUID());
		}
		playerNames = new HashMap<>();
		partialZones = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		switch (label) {
			case "corner1":
				corner(sender, true);
				break;
			case "corner2":
				corner(sender, false);
				break;
			case "cancel":
				cancel(sender);
				break;
			case "land":
				land(sender);
				break;
			case "protect":
				protect(sender, args);
				break;
			case "unprotect":
				unprotect(sender, args);
				break;
			case "add":
				add(sender, args);
			case "remove":
				remove(sender, args);
		}
		return super.onCommand(sender, command, label, args);
	}

	public void corner(CommandSender sender, boolean is1) {
		Player player = (Player) sender;
		UUID pUID = player.getUniqueId();
		if(!partialZones.containsKey(pUID)) {
			partialZones.put(pUID, new PartialZone());
		}
		PartialZone partialZone = partialZones.get(pUID);
		Point point = new Point(
			player.getLocation().getBlockX(),
			player.getLocation().getBlockY()-1,
			player.getLocation().getBlockZ()
		);
		UUID wUID = player.getWorld().getUID();
		if (landData.getZone(wUID, point).isPresent()) {
			player.sendMessage("You cannot set a new corner in an already existing zone. (/land for more info)");
		} else {
			if(is1) {
				partialZone.corner1 = point;
			} else {
				partialZone.corner2 = point;
			}
			String msgCorner = "Corner %c was set to %s \nUse /corner%c to set the opposite corner of your zone";
			if(partialZone.corner1 == null) {
				player.sendMessage(String.format(msgCorner, '2', point.toString(), '1'));
			} else if (partialZone.corner2 == null) {
				player.sendMessage(String.format(msgCorner, '1', point.toString(), '2'));
			} else {
				Optional<Zone> overlap = landData.getZone(wUID, partialZone);
				if(overlap.isPresent()) {
					String ownerName = playerNames.get(overlap.get().owner);
					if (is1) {
						partialZone.corner1 = null;
					} else {
						partialZone.corner2 = null;
					}
					player.sendMessage("Sorry but this zone is overlaping with a zone owned by "+ownerName+", check your corners");
				} else {
					player.sendMessage(
						"Your zone was succesfully defined (volume: " + new Zone(partialZone.corner1, partialZone.corner2).getVolume()
						+ ") ! Use /protect to grant it a protection and register it for good"
					);
				}
			}
		}
	}

	public void cancel(CommandSender sender) {
		Player player = (Player) sender;
		UUID pUID = player.getUniqueId();
		if(partialZones.containsKey(pUID)) {
			partialZones.remove(pUID);
			player.sendMessage("Succesfully deleted your corners");
		} else {
			player.sendMessage("You had no corner to delete");
		}
	}

	public void land(CommandSender sender) {
		Player player = (Player) sender;
		Point point = new Point(
				player.getLocation().getBlockX(),
				player.getLocation().getBlockY(),
				player.getLocation().getBlockZ()
		);
		UUID wUID = player.getWorld().getUID();
		Optional<Zone> zoneOpt = this.landData.getZone(wUID, point);
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
			player.sendMessage("You're not in a claimed land");
		}
	}

	private void protectMsg(StringBuilder msg, Player player, Protections[] protections, int volume) {
		double amount;
		Currencies currency;
		for(Protections protec: protections) {
			amount = protec.price.amount*volume;
			currency = protec.price.currency;
			msg.append(String.format("  - %s for %s \n", protec.toString(), currency.toString(amount)));
		}
	}

	private Zone getOwnedZone(Player player) {
		UUID pUID = player.getUniqueId();
		UUID wUID = player.getWorld().getUID();
		Zone zone;
		if(partialZones.containsKey(pUID)) {
			zone = new Zone(partialZones.get(pUID));
		} else {
			Point point = new Point(
				player.getLocation().getBlockX(),
				player.getLocation().getBlockY(),
				player.getLocation().getBlockZ()
			);
			Optional<Zone> zone_opt = landData.getZone(wUID, pUID, point);
			if (!zone_opt.isPresent()) {
				throw new ValueException("The player is not building a zone and is not standing in a zone he owns");
			}
			zone = zone_opt.get();
		}
		return zone;
	}

	public void protect(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		try {
			Zone zone = getOwnedZone(player);
			if(args.length == 0) {
				StringBuilder msg = new StringBuilder();
				// make a list with the lacking protection
				Protections[] protecs = (Protections[]) zone.protecs.keySet().stream().filter(p->!zone.protecs.get(p)).toArray();
				if (protecs.length == 0) {
					player.sendMessage("Your zone is fully protected");
				} else {
					msg.append("Here are the protections you can grant your zone:\n");
					protectMsg(msg, player, protecs, zone.getVolume());
					msg.append("To grant one, place the amount to pay in your hotbar and use /protect <protection>");
					player.sendMessage(msg.toString());
				}
			} else {
				// TODO: this step
				player.sendMessage("This action isn't available yet, be nice with Inspi plz");
			}
		} catch (ValueException e) {
			player.sendMessage("You must be in a zone you own to use /protect (/land for more info)");
		}
	}

	public void unprotect(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		try {
			Zone zone = getOwnedZone(player);
			if(args.length == 0) {
				StringBuilder msg = new StringBuilder();
				// make a list with the lacking protection
				Protections[] protecs = (Protections[]) zone.protecs.keySet().stream().filter(p->zone.protecs.get(p)).toArray();
				if (protecs.length == 0) {
					player.sendMessage("Your zone has no protection yet");
				} else {
					msg.append("Here are the protections you can remove from your zone:\n");
					protectMsg(msg, player, protecs, zone.getVolume());
					msg.append("To remove one, use /unprotect <protection>, you will be refunded by the amount displayed");
					player.sendMessage(msg.toString());
				}
			} else {
				// TODO: this step
				player.sendMessage("This action isn't available yet, be nice with Inspi plz");
			}
		} catch (ValueException e) {
			player.sendMessage("You must be in a zone you own to use /unprotect (/land for more info)");
		}
	}

	public void add(CommandSender sender, String[] args) {
	}

	public void remove(CommandSender sender, String[] args) {
	}
}
