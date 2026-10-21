package com.example.threelives;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BoxCommand implements CommandExecutor {

    private final ThreeLivesPlugin plugin;

    public BoxCommand(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("threelives.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou do not have permission.");
            return true;
        }

        plugin.getBoxManager().setGlobalBoxLocation(player.getLocation());
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§aGlobal death box location set! The 5x5 box has been generated around you.");

        return true;
    }
}
