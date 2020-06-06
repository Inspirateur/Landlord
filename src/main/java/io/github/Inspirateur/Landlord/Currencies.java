package io.github.Inspirateur.Landlord;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

enum Currencies {
	iron(Material.IRON_INGOT, Material.IRON_BLOCK),
	gold(Material.GOLD_INGOT, Material.GOLD_BLOCK),
	// emerald(Material.EMERALD, Material.EMERALD_BLOCK),
	diamond(Material.DIAMOND, Material.DIAMOND_BLOCK);

	// An <blockID> is worth <multiplier> <itemID>
	public final Material item;
	public final Material block;
	public final int multiplier;

	Currencies(Material item, Material block) {
		this(item, block, 9);
	}

	Currencies(Material item, Material block, int multiplier) {
		this.item = item;
		this.block = block;
		this.multiplier = multiplier;
	}

	public void pay(Player player, int amount) throws NotEnoughMoneyException {
		// scan the inventory
		List<Integer> indices = new ArrayList<>();
		int amountTemp = amount;
		for(int i = 0; i < player.getInventory().getSize(); i++){
			ItemStack pItem = player.getInventory().getItem(i);
			// make sure the item is not null
			if(pItem != null) {
				if(pItem.getType().equals(item)) {
					amountTemp -= pItem.getAmount();
					indices.add(i);
				} else if(pItem.getType().equals(block)) {
					amountTemp -= pItem.getAmount()*multiplier;
					indices.add(i);
				}
				if(amountTemp <= 0) {
					break;
				}
			}
		}
		if(amountTemp > 0) {
			throw new NotEnoughMoneyException();
		}
		for(Integer i : indices) {
			player.getInventory().setItem(i, null);
		}
		if (amountTemp < 0) {
			give(player, amountTemp*-1);
		}
	}

	public void give(Player player, int amount) throws InventoryFullException {
		int amountTemp = amount;
		List<ItemStack> toGive = new ArrayList<>();
		int n;
		while (amountTemp > 0) {
			// choose wether to pay with blocks or items
			if(amountTemp % multiplier == 0 || amountTemp > item.getMaxStackSize()) {
				n = Math.min(amountTemp/multiplier, block.getMaxStackSize());
				toGive.add(new ItemStack(block, n));
				amountTemp -= n*multiplier;
			} else {
				n = Math.min(amountTemp, item.getMaxStackSize());
				toGive.add(new ItemStack(item, n));
				amountTemp -= n;
			}
		}
		// assert toGive.size() > 0;
		List<Integer> indices = new ArrayList<>();
		for(int i = 0; i < player.getInventory().getSize(); i++){
			ItemStack pItem = player.getInventory().getItem(i);
			// make sure the item is not null
			if(pItem == null) {
				indices.add(i);
				if(indices.size() == toGive.size()) {
					break;
				}
			}
		}
		if(indices.size() < toGive.size()) {
			throw new InventoryFullException();
		}
		for(int i=0; i<indices.size(); i++) {
			player.getInventory().setItem(indices.get(i), toGive.get(i));
		}
	}

	public String toString(double amount) {
		StringBuilder msg = new StringBuilder();
		msg.append(String.format("%d %s", (int)amount, this.toString()));
		if (amount < multiplier) {
			return msg.toString();
		}
		int blockAmount = (int)amount/multiplier;
		int itemAmount = (int)amount-blockAmount*multiplier;
		msg.append(String.format(
			" or %d %s %d %s", blockAmount, block.name().replace('_', ' '),
			itemAmount, this.toString()
		));
		return msg.toString();
	}
}
