package net.velinquish.armorsets;

import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import net.velinquish.utils.Common;

public class ReloadCommand extends Command {

	ArmorSets plugin;

	protected ReloadCommand(ArmorSets plugin) {
		super(plugin.getConfig().getString("main-command"));
		setAliases(plugin.getConfig().getStringList("plugin-aliases"));
		setDescription("Main command for ArmorSets");
		setUsage("/armorsets <reload>");

		this.plugin = plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if (args.length < 1 || !"reload".equalsIgnoreCase(args[0]))
			plugin.getLangManager().getNode("command-message").execute(sender);
		else {
			String perm = plugin.getPermission().replaceAll("%action%", "reload");
			if (!sender.hasPermission(perm)) {
				plugin.getLangManager().getNode("no-permission").replace(Common.map("%permission%", perm)).execute(sender);
				return false;
			}

			try {
				plugin.loadFiles();
			} catch (IOException | InvalidConfigurationException e) {
				e.printStackTrace();
				Common.tell(sender, "&cAn error occurred when reloading the files.");
				return false;
			}
			plugin.getLangManager().getNode("plugin-reloaded").execute(sender);
		}
		return false;
	}

}
