package io.github.Inspirateur.Landlord;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	private LandData landData;

	@Override
	public void onEnable() {
		getLogger().info("onEnable is called!");
		landData = new LandData();
	}
}
