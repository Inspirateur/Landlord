package io.github.Inspirateur.Landlord;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Plugin, Listener {
	private LandData landData;
	private PlayerCache playerCache;
	// <world, <player, partialZone>>
	private Map<UUID, PartialZone> partialZones;

	@Override
	public void onEnable() {
		landData = new LandData();
		for (World w : getServer().getWorlds()) {
			landData.registerWorld(w.getUID());
		}
		playerCache = new PlayerCache();
		partialZones = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, this);
		getServer().getScheduler().scheduleSyncRepeatingTask(
			this, new ZoneParticles(partialZones), 0L, 15L
		);
		getLogger().info("Landlord Ready");
	}

	@Override
	public void onDisable() {
		landData.save();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		playerCache.update(player);
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			if (
				(event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() instanceof Player) ||
				event.getDamager() instanceof Player
			) {
				Player player = (Player) event.getEntity();
				UUID wUID = player.getWorld().getUID();
				Point point = new Point(
					player.getLocation().getBlockX(),
					player.getLocation().getBlockY(),
					player.getLocation().getBlockZ()
				);
				Optional<Zone> zone = landData.getZone(wUID, point);
				if(zone.isPresent() && zone.get().protecs.get(Protections.PVP)) {
					event.setCancelled(true);
				}
			}
		}
	}

	private void onPlayerGrief(Cancellable event, Player player, Block block) {
		Point point = new Point(
			block.getX(), block.getY(), block.getZ()
		);
		UUID wUID = block.getWorld().getUID();
		Optional<Zone> zoneOpt = landData.getZone(wUID, point);
		if (zoneOpt.isPresent()) {
			Zone zone = zoneOpt.get();
			UUID pUID = player.getUniqueId();
			if (zone.protecs.get(Protections.playerGrief) && !zone.owner.equals(pUID) && !zone.guests.contains(pUID)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		onPlayerGrief(event, event.getPlayer(), event.getBlock());
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		onPlayerGrief(event, event.getPlayer(), event.getBlock());
	}

	@EventHandler
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		onPlayerGrief(event, event.getPlayer(), event.getBlock());
	}

	@EventHandler
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		onPlayerGrief(event, event.getPlayer(), event.getBlock());
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e) {
		Entity entity = e.getEntity();
		Point point = new Point(
			entity.getLocation().getBlockX(),
			entity.getLocation().getBlockY(),
			entity.getLocation().getBlockZ()
		);
		UUID wUID = entity.getWorld().getUID();
		Optional<Zone> zoneOpt = landData.getZone(wUID, point);
		if (zoneOpt.isPresent() && zoneOpt.get().protecs.get(Protections.mobGrief)) {
			e.blockList().clear();
		}
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
		switch (label) {
			case "corner1" -> corner(sender, true);
			case "corner2" -> corner(sender, false);
			case "cancel" -> cancel(sender);
			case "land" -> land(sender);
			case "protect" -> protect(sender, args);
			case "unprotect" -> unprotect(sender, args);
			case "add" -> addOrRemove(sender, true, args);
			case "remove" -> addOrRemove(sender, false, args);
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (label.equals("protect") || label.equals("unprotect")) {
			Player player = (Player) sender;
			try {
				Zone zone = getOwnedZone(player);
				if (label.equals("protect")) {
					// a list with the lacking protections
					return zone.protecs.keySet().stream().filter(p -> !zone.protecs.get(p)).map(Enum::toString).toList();
				} else {
					// a list with the existing protections
					return zone.protecs.keySet().stream().filter(p -> zone.protecs.get(p)).map(Enum::toString).toList();
				}
			} catch (NotInOwnZoneException ignored) {}
		}
		return new ArrayList<>();
	}

	public void corner(CommandSender sender, boolean isCorner1) {
		Player player = (Player) sender;
		UUID pUID = player.getUniqueId();
		if (!partialZones.containsKey(pUID)) {
			partialZones.put(pUID, new PartialZone());
		}
		PartialZone partialZone = partialZones.get(pUID);
		Point point = new Point(
			player.getLocation().getBlockX(),
			player.getLocation().getBlockY() - 1,
			player.getLocation().getBlockZ()
		);
		World world = player.getWorld();
		UUID wUID = world.getUID();
		if (landData.getZone(wUID, point).isPresent()) {
			player.sendMessage("You cannot set a new corner in an already existing zone. (/land for more info)");
		} else {
			if (isCorner1) {
				partialZone.corner1 = point;
			} else {
				partialZone.corner2 = point;
			}
			String msgCorner = "Corner %c was set to %s \nUse /corner%c to set the opposite corner of your zone";
			if (partialZone.corner1 == null) {
				partialZone.world = world;
				player.sendMessage(String.format(msgCorner, '2', point, '1'));
			} else if (partialZone.corner2 == null) {
				partialZone.world = world;
				player.sendMessage(String.format(msgCorner, '1', point, '2'));
			} else {
				Optional<Zone> overlap = landData.getZone(wUID, partialZone);
				if (overlap.isPresent()) {
					String ownerName = playerCache.get(overlap.get().owner);
					if (isCorner1) {
						partialZone.corner1 = null;
					} else {
						partialZone.corner2 = null;
					}
					player.sendMessage("Sorry but this zone is overlaping with a zone owned by " + ownerName + ", check your corners");
				} else if (world.getUID() != wUID) {
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
		if (partialZones.containsKey(pUID)) {
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
					zone.guests.stream().map(g -> playerCache.get(g)).collect(Collectors.joining(", "))
				).append("\n");
			}
			msg.append("Protections: ").append(
				zone.protecs.entrySet().stream().filter(Map.Entry::getValue)
					.map(p -> p.getKey().toString()).collect(Collectors.joining(", "))
			).append("\n");
			msg.append("Volume: ").append(zone.getVolume()).append("\n");
			player.sendMessage(msg.toString());
		} else {
			player.sendMessage("You're not in a claimed land");
		}
	}

	private void protectMsg(StringBuilder msg, List<Protections> protections, int volume) {
		double amount;
		Currencies currency;
		for (Protections protec : protections) {
			amount = Math.max(protec.price.amount * volume, 1);
			currency = protec.price.currency;
			msg.append(String.format("  - %s for %s \n", protec, currency.toString(amount)));
		}
	}

	private Zone getOwnedZone(Player player) {
		UUID pUID = player.getUniqueId();
		UUID wUID = player.getWorld().getUID();
		Zone zone;
		if (partialZones.containsKey(pUID)) {
			zone = new Zone(partialZones.get(pUID));
		} else {
			Point point = new Point(
				player.getLocation().getBlockX(),
				player.getLocation().getBlockY(),
				player.getLocation().getBlockZ()
			);
			Optional<Zone> zone_opt = landData.getZone(wUID, pUID, point);
			if (zone_opt.isEmpty()) {
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
			if (args.length == 0) {
				StringBuilder msg = new StringBuilder();
				// make a list with the lacking protections
				List<Protections> protecs = zone.protecs.keySet().stream().filter(p -> !zone.protecs.get(p)).toList();
				if (protecs.size() == 0) {
					player.sendMessage("Your zone is fully protected");
				} else {
					msg.append("Here are the protections you can grant your zone:\n");
					protectMsg(msg, protecs, zone.getVolume());
					msg.append("To grant one, place the amount to pay in your hotbar and use /protect <protection>");
					player.sendMessage(msg.toString());
				}
			} else {
				for (String protectionName : args) {
					Protections protection;
					try {
						protection = Protections.valueOf(protectionName);
					} catch (RuntimeException e) {
						player.sendMessage("Protection " + protectionName + " does not exist");
						return;
					}
					if(zone.protecs.get(protection)) {
						player.sendMessage("This zone already has " + protectionName + " protection");
					} else {
						try {
							int amount = Math.max((int)(zone.getVolume()*protection.price.amount), 1);
							protection.price.currency.pay(player, amount);
							zone.protecs.put(protection, true);
							player.sendMessage("Your zone was succesfully protected from " + protectionName);
							// checks if it was an unregistered zone
							if(zone.owner == null) {
								UUID pUID = player.getUniqueId();
								World world = partialZones.get(pUID).world;
								partialZones.remove(pUID);
								zone.owner = pUID;
								landData.addZone(world.getUID(), zone);
								player.sendMessage("New zone registered");
							}
						} catch (NotEnoughMoneyException e) {
							player.sendMessage("You don't have enough money (/protect for more info)");
						}
					}
				}
			}
		} catch (NotInOwnZoneException e) {
			player.sendMessage("You must be in a zone you own to use /protect (/land for more info)");
		}
	}

	public void unprotect(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		try {
			Zone zone = getOwnedZone(player);
			if (args.length == 0) {
				StringBuilder msg = new StringBuilder();
				// make a list with the lacking protection
				List<Protections> protecs = zone.protecs.keySet().stream().filter(p -> zone.protecs.get(p)).collect(Collectors.toList());
				if (protecs.size() == 0) {
					player.sendMessage("Your zone has no protection yet");
				} else {
					msg.append("Here are the protections you can remove from your zone:\n");
					protectMsg(msg, protecs, zone.getVolume());
					msg.append("To remove one, use /unprotect <protection>, you will be refunded by the amount displayed");
					player.sendMessage(msg.toString());
				}
			} else {
				for (String protectionName : args) {
					try {
						Protections protection = Protections.valueOf(protectionName);
						if(!zone.protecs.get(protection)) {
							player.sendMessage("This zone doesn't have " + protectionName + " protection");
						} else {
							try {
								int amount = Math.max((int)(zone.getVolume()*protection.price.amount), 1);
								protection.price.currency.give(player, amount);
								zone.protecs.put(protection, false);
								player.sendMessage("Protection " + protectionName + " was succesfully removed from your zone");
								// checks if there is any protection left
								if(zone.protecs.values().stream().filter(v->v).toArray().length == 0) {
									landData.removeZone(player.getWorld().getUID(), player.getUniqueId(), zone);
									player.sendMessage("Since this zone has no protection left it was deleted");
								}
							} catch (InventoryFullException e) {
								player.sendMessage("You don't have enough space in your inventory, make some room and try again");
							}
						}
					} catch (RuntimeException e) {
						player.sendMessage("Protection " + protectionName + " does not exist");
					}
				}
			}
		} catch (NotInOwnZoneException e) {
			player.sendMessage("You must be in a zone you own to use /unprotect (/land for more info)");
		}
	}

	public void addOrRemove(CommandSender sender, boolean isAdd, String[] args) {
		Player player = (Player) sender;
		String cmd = isAdd ? "add" : "remove";
		try {
			Zone zone = getOwnedZone(player);
			if (zone.owner != null) {
				if (args.length > 0) {
					for (String pName : args) {
						if (player.getName().equals(pName)) {
							if(isAdd) {
								player.sendMessage("You cannot add yourself to your zone");
							} else {
								player.sendMessage("You cannot remove yourself from your zone");
							}
						} else if (playerCache.contains(pName)) {
							UUID pUID = playerCache.get(pName);
							if (isAdd) {
								zone.guests.add(pUID);
								player.sendMessage("Player " + pName + " was added to your zone");
							} else {
								zone.guests.remove(pUID);
								player.sendMessage("Player " + pName + " was removed from your zone");
							}
						} else {
							player.sendMessage("Sorry I don't know any " + pName);
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
