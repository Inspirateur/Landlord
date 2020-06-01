package io.github.Inspirateur.Landlord;

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
	private PlayerCache playerCache;
	// <world, <player, partialZone>>
	private Map<UUID, PartialZone> partialZones;

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		playerCache.update(player);
	}

	@Override
	public void onEnable() {
		getLogger().info("onEnable is called!");
		landData = new LandData();
		for(World w: getServer().getWorlds()) {
			landData.registerWorld(w.getUID());
		}
		playerCache = new PlayerCache();
		partialZones = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, this);
		int zoneParticlesTask = getServer().getScheduler().scheduleSyncRepeatingTask(
			this, new ZoneParticles(partialZones), 0L, 20L
		);
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
				addOrRemove(sender, true, args);
			case "remove":
				addOrRemove(sender, false, args);
		}
		return super.onCommand(sender, command, label, args);
	}

	public void corner(CommandSender sender, boolean isCorner1) {
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
		World world = player.getWorld();
		UUID wUID = world.getUID();
		if (landData.getZone(wUID, point).isPresent()) {
			player.sendMessage("You cannot set a new corner in an already existing zone. (/land for more info)");
		} else {
			if(isCorner1) {
				partialZone.corner1 = point;
			} else {
				partialZone.corner2 = point;
			}
			String msgCorner = "Corner %c was set to %s \nUse /corner%c to set the opposite corner of your zone";
			if(partialZone.corner1 == null) {
				partialZone.world = world;
				player.sendMessage(String.format(msgCorner, '2', point.toString(), '1'));
			} else if (partialZone.corner2 == null) {
				partialZone.world = world;
				player.sendMessage(String.format(msgCorner, '1', point.toString(), '2'));
			} else {
				Optional<Zone> overlap = landData.getZone(wUID, partialZone);
				if(overlap.isPresent()) {
					String ownerName = playerCache.get(overlap.get().owner);
					if (isCorner1) {
						partialZone.corner1 = null;
					} else {
						partialZone.corner2 = null;
					}
					player.sendMessage("Sorry but this zone is overlaping with a zone owned by "+ownerName+", check your corners");
				} else if(world.getUID() != wUID) {
					if (isCorner1) {
						partialZone.corner1 = null;
					} else {
						partialZone.corner2 = null;
					}
					player.sendMessage("LOL, no you cannot set your corners in a different world");
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
			msg.append("Zone Owner: ").append(playerCache.get(zone.owner)).append("\n");
			if (zone.guests.size() > 0) {
				msg.append("Guests: ").append(
					zone.guests.stream().map(g->playerCache.get(g)).collect(Collectors.joining(", "))
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

	private void protectMsg(StringBuilder msg, Protections[] protections, int volume) {
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
				throw new NotInOwnZoneException();
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
					protectMsg(msg, protecs, zone.getVolume());
					msg.append("To grant one, place the amount to pay in your hotbar and use /protect <protection>");
					player.sendMessage(msg.toString());
				}
			} else {
				// TODO: this step
				player.sendMessage("This action isn't available yet, be nice with Inspi plz");
			}
		} catch (NotInOwnZoneException e) {
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
					protectMsg(msg, protecs, zone.getVolume());
					msg.append("To remove one, use /unprotect <protection>, you will be refunded by the amount displayed");
					player.sendMessage(msg.toString());
				}
			} else {
				// TODO: this step
				player.sendMessage("This action isn't available yet, be nice with Inspi plz");
			}
		} catch (NotInOwnZoneException e) {
			player.sendMessage("You must be in a zone you own to use /unprotect (/land for more info)");
		}
	}

	public void addOrRemove(CommandSender sender, boolean isAdd, String[] args) {
		Player player = (Player) sender;
		String cmd = isAdd? "add" : "remove";
		try {
			Zone zone = getOwnedZone(player);
			if(zone.owner != null) {
				if (args.length > 0) {
					for(String pName: args) {
						if(playerCache.contains(pName)) {
							UUID pUID = playerCache.get(pName);
							if(isAdd) {
								zone.guests.add(pUID);
								player.sendMessage("Player "+pName+" was added to your zone");
							} else {
								zone.guests.remove(pUID);
								player.sendMessage("Player "+pName+" was removed from your zone");
							}
						} else {
							player.sendMessage("Sorry I don't know any "+pName);
						}
					}
				} else {
					player.sendMessage(String.format("Usage: /%s <Player>", cmd));
				}
			} else {
				player.sendMessage(String.format(
					"Your zone is not registered yet, grant it one protection before using /%s (/protect for more info)",
					cmd
				));
			}
		} catch (NotInOwnZoneException e) {
			player.sendMessage(String.format(
				"You need to be in a zone you own to use /%s <Player> (/land for more info)",
				cmd
			));
		}
	}
}
