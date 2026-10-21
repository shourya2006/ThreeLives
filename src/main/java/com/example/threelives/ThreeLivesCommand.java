package com.example.threelives;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ThreeLivesCommand implements CommandExecutor {

    private final ThreeLivesPlugin plugin;

    public ThreeLivesCommand(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("threelives.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission());
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aConfiguration and data reloaded successfully!");
            return true;
        }

        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /threelives reload");
        return true;
    }
}
