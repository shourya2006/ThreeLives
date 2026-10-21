package com.example.threelives;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all ThreeLives commands:
 *   /lives [player]
 *   /setlives <player> <amount>
 *   /resetlives <player>
 *   /addlives <player> <amount>
 *   /removelives <player> <amount>
 */
public class LivesCommand implements CommandExecutor, TabCompleter {

    private final ThreeLivesPlugin plugin;

    public LivesCommand(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();
        LivesDataManager data = plugin.getLivesDataManager();
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {

            // ── /lives [player] ───────────────────────────────────────────────────
            case "lives" -> {
                if (!sender.hasPermission("threelives.lives")) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgNoPermission());
                    return true;
                }
                if (args.length == 0) {
                    // Check own lives (must be a player)
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(cfg.getPrefix() + "&cConsole must specify a player: /lives <player>");
                        return true;
                    }
                    int lives = data.getLives(player);
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesCheckSelf()
                            .replace("{lives}", String.valueOf(lives)));
                } else {
                    // Check another player
                    Player target = Bukkit.getPlayerExact(args[0]);
                    if (target == null) {
                        sender.sendMessage(cfg.getPrefix() + cfg.getMsgPlayerNotFound()
                                .replace("{player}", args[0]));
                        return true;
                    }
                    int lives = data.getLives(target);
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesCheckOther()
                            .replace("{player}", target.getName())
                            .replace("{lives}", String.valueOf(lives)));
                }
            }

            // ── /setlives <player> <amount> ────────────────────────────────────────
            case "setlives" -> {
                if (!sender.hasPermission("threelives.setlives")) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgNoPermission());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(cfg.getPrefix() + "&cUsage: /setlives <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgPlayerNotFound()
                            .replace("{player}", args[0]));
                    return true;
                }
                int amount = parseIntOrSend(sender, args[1], cfg);
                if (amount == -999) return true;

                int previousLives = data.getLives(target);
                data.setLives(target, amount);
                plugin.getPlayerListener().synchronizeLives(target, previousLives);
                sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesSet()
                        .replace("{player}", target.getName())
                        .replace("{lives}", String.valueOf(data.getLives(target))));
            }

            // ── /resetlives <player> ──────────────────────────────────────────────
            case "resetlives" -> {
                if (!sender.hasPermission("threelives.resetlives")) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgNoPermission());
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(cfg.getPrefix() + "&cUsage: /resetlives <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgPlayerNotFound()
                            .replace("{player}", args[0]));
                    return true;
                }
                int previousLives = data.getLives(target);
                int newLives = data.resetLives(target.getUniqueId());
                plugin.getPlayerListener().synchronizeLives(target, previousLives);
                sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesReset()
                        .replace("{player}", target.getName())
                        .replace("{lives}", String.valueOf(newLives)));
            }

            // ── /addlives <player> <amount> ────────────────────────────────────────
            case "addlives" -> {
                if (!sender.hasPermission("threelives.addlives")) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgNoPermission());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(cfg.getPrefix() + "&cUsage: /addlives <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgPlayerNotFound()
                            .replace("{player}", args[0]));
                    return true;
                }
                int amount = parseIntOrSend(sender, args[1], cfg);
                if (amount == -999) return true;
                if (amount <= 0) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgInvalidNumber());
                    return true;
                }

                int currentLives = data.getLives(target);
                if (currentLives >= cfg.getMaxLives()) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesMaxed()
                            .replace("{max}", String.valueOf(cfg.getMaxLives())));
                    return true;
                }
                int newLives = data.addLives(target.getUniqueId(), amount);
                plugin.getPlayerListener().synchronizeLives(target, currentLives);
                sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesAdd()
                        .replace("{player}", target.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{lives}", String.valueOf(newLives)));
            }

            // ── /removelives <player> <amount> ────────────────────────────────────
            case "removelives" -> {
                if (!sender.hasPermission("threelives.removelives")) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgNoPermission());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(cfg.getPrefix() + "&cUsage: /removelives <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgPlayerNotFound()
                            .replace("{player}", args[0]));
                    return true;
                }
                int amount = parseIntOrSend(sender, args[1], cfg);
                if (amount == -999) return true;
                if (amount <= 0) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgInvalidNumber());
                    return true;
                }
                if (data.getLives(target) <= 0) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesAlreadyZero());
                    return true;
                }
                int previousLives = data.getLives(target);
                int newLives = data.removeLives(target.getUniqueId(), amount);
                plugin.getPlayerListener().synchronizeLives(target, previousLives);
                sender.sendMessage(cfg.getPrefix() + cfg.getMsgLivesRemove()
                        .replace("{player}", target.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{lives}", String.valueOf(newLives)));
            }

            // ── /withdraw [amount] ──────────────────────────────────────────────
            case "withdraw" -> {
                if (!cfg.isEnableWithdrawCommand()) {
                    sender.sendMessage(cfg.getPrefix() + "§cThe withdraw command is disabled.");
                    return true;
                }
                if (!sender.hasPermission("threelives.withdraw")) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgNoPermission());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(cfg.getPrefix() + "&cOnly players can withdraw lives.");
                    return true;
                }

                int amount = 1;
                if (args.length >= 1) {
                    amount = parseIntOrSend(sender, args[0], cfg);
                    if (amount == -999) return true;
                }

                if (amount <= 0) {
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgInvalidNumber());
                    return true;
                }

                int currentLives = data.getLives(player);
                if (currentLives <= amount) {
                    // Cannot withdraw if it would leave them at 0 or less lives
                    sender.sendMessage(cfg.getPrefix() + cfg.getMsgWithdrawNotEnough()
                            .replace("{required}", String.valueOf(amount + 1)));
                    return true;
                }

                int newLives = data.removeLives(player.getUniqueId(), amount);
                plugin.getNametagManager().refreshPlayer(player);

                // Give the custom item
                org.bukkit.inventory.ItemStack lifeItem = plugin.getWithdrawManager().createLifeItem(amount);
                
                // Add to inventory, drop excess on floor
                player.getInventory().addItem(lifeItem).values().forEach(
                        excess -> player.getWorld().dropItem(player.getLocation(), excess)
                );

                sender.sendMessage(cfg.getPrefix() + cfg.getMsgWithdrawSuccess()
                        .replace("{amount}", String.valueOf(amount)));
            }

            default -> sender.sendMessage(cfg.getPrefix() + "&cUnknown command.");
        }
        return true;
    }

    // ── Tab Completion ────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        if (args.length == 1) {
            // Complete player names for all commands that take a player
            String partial = args[0].toLowerCase();
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .forEach(completions::add);
        } else if (args.length == 2 && (cmdName.equals("setlives") || cmdName.equals("addlives") || cmdName.equals("removelives"))) {
            // Suggest amounts
            completions.addAll(List.of("1", "2", "3"));
        }
        return completions;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Parse integer from string or send error to sender.
     * Returns -999 as sentinel on failure.
     */
    private int parseIntOrSend(CommandSender sender, String s, ConfigManager cfg) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            sender.sendMessage(cfg.getPrefix() + cfg.getMsgInvalidNumber());
            return -999;
        }
    }
}
