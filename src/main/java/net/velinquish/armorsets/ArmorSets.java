package net.velinquish.armorsets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import net.velinquish.armorsets.armorequip.ArmorEquipEvent;
import net.velinquish.armorsets.armorequip.ArmorEquipEvent.EquipMethod;
import net.velinquish.armorsets.armorequip.ArmorListener;
import net.velinquish.armorsets.armorequip.ArmorType;
import net.velinquish.utils.Common;
import net.velinquish.utils.VelinquishPlugin;
import net.velinquish.utils.lang.LangManager;

public class ArmorSets extends JavaPlugin implements Listener, VelinquishPlugin {

	@Getter
	private static ArmorSets instance;
	@Getter
	private LangManager langManager;

	@Getter
	private String prefix;
	@Getter
	private String permission;

	private static boolean debug;

	@Getter
	private YamlConfiguration config;
	private File configFile;

	@Getter
	private YamlConfiguration lang;
	private File langFile;

	private YamlConfiguration armorSets;
	private File armorSetsFile;

	// Set names, Commands to execute when activating the set
	private Map<List<String>, List<String>> sets;
	// Commands to execute when disabling the sets
	private List<String> disabling;

	@Override
	public void onEnable() {
		instance = this;
		Common.setInstance(this);

		sets = new HashMap<>();
		langManager = new LangManager();

		try {
			loadFiles();
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
		getServer().getPluginManager().registerEvents(this, this);
		Common.registerCommand(new ReloadCommand(this));
	}

	@Override
	public void onDisable() {
		instance = null;
	}

	public void loadFiles() throws IOException, InvalidConfigurationException {
		configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			saveResource("config.yml", false);
		}
		config = new YamlConfiguration();
		config.load(configFile);

		prefix = getConfig().getString("plugin-prefix");
		debug = getConfig().getBoolean("debug");
		permission = getConfig().getString("permission");

		langFile = new File(getDataFolder(), "lang.yml");
		if (!langFile.exists()) {
			langFile.getParentFile().mkdirs();
			saveResource("lang.yml", false);
		}
		lang = new YamlConfiguration();
		lang.load(langFile);

		langManager.clear();
		langManager.setPrefix(prefix);
		langManager.loadLang(lang);

		armorSetsFile = new File(getDataFolder(), "armorsets.yml");
		if (!armorSetsFile.exists()) {
			armorSetsFile.getParentFile().mkdirs();
			saveResource("armorsets.yml", false);
		}
		armorSets = new YamlConfiguration();
		armorSets.load(armorSetsFile);

		loadArmorSets();
	}

	public void loadArmorSets() {
		disabling = armorSets.getStringList("disabling-commands");
		for (String set : armorSets.getConfigurationSection("sets").getKeys(false)) {
			sets.put(armorSets.getStringList("sets." + set + ".name-contains"), armorSets.getStringList("sets." + set + ".commands"));
			debug("Loaded armor set " + set + ".");
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onArmorEquip(ArmorEquipEvent e) {

		Player player = e.getPlayer();

		Bukkit.getScheduler().runTask(this, () -> {
			ItemStack[] armors = player.getInventory().getArmorContents();
			nameCheck:
				for (List<String> set : sets.keySet())
					for (int i = 0; i < armors.length; i++) {
						if (armors[i] == null || !armors[i].getItemMeta().hasDisplayName())
							break nameCheck; //Goes on to removal
						boolean found = false;
						for (String name : set)
							if (armors[i].getItemMeta().getDisplayName().contains(name))
								if (i != armors.length - 1) {
									found = true;
									break; //Continues to the next piece of armor
								} else {
									//Adds permission and ends
									for (String command : sets.get(set))
										Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("%player%", player.getName()));
									debug("Executing set " + set + " commands");
									return;
								}
						if (!found)
							break; //Goes on to checking the next set
					}
			//Removes all permissions from all sets
			for (String command : disabling)
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("%player%", player.getName()));
			debug("Disabling sets");
		});
		return;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerRightClickEquip(PlayerInteractEvent e) {
		ItemStack item = e.getPlayer().getInventory().getItemInMainHand();

		if (!item.hasItemMeta() || !item.getType().isBlock() || !item.getItemMeta().hasDisplayName())
			return;

		if (e.getPlayer().getInventory().getHelmet() != null)
			return;

		for (List<String> set : sets.keySet())
			for (String name : set)
				if (item.getItemMeta().getDisplayName().contains(name)) {
					e.setCancelled(true);
					e.getPlayer().getInventory().setItemInMainHand(null);
					e.getPlayer().getInventory().setHelmet(item);
					return;
				}
	}

	//TODO Try InventoryDragEvent, as well?
	//TODO Try installing MassiveHats to see how that works with the same event
	@EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
	public void onPlayerPlaceBlockHelmet(final InventoryClickEvent e) {
		debug("An inventory click event was called");
		if (!(e.getWhoClicked() instanceof Player)) return;

		Player player = (Player) e.getWhoClicked();

		InventoryView view = e.getView();

		if (view.getType() != InventoryType.CRAFTING) return;

		if (e.getRawSlot() != 5) return; //Is not the helmet slot

		final ItemStack cursor = e.getCursor();

		if (cursor == null || cursor.getAmount() == 0 || cursor.getType() == Material.AIR) return;

		if (!cursor.getType().isBlock()) return;

		e.setResult(Event.Result.DENY);

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
			final ItemStack current = e.getCurrentItem();

			e.setCurrentItem(cursor);
			view.setCursor(current);

			player.updateInventory();

			onArmorEquip(new ArmorEquipEvent(player, EquipMethod.DRAG, ArmorType.HELMET, current, cursor));
			debug("All checks succeeded! The ArmorRquipEvent was called after dragging and equipping the block helmet!");
		});
	}

	public static void debug(String message) {
		if (debug == true)
			Common.log(message);
	}
}
