package com.example.threelives;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Reads and caches all settings from config.yml.
 */
public class ConfigManager {

    private final ThreeLivesPlugin plugin;

    // Lives settings
    private int defaultLives;
    private int maxLives;
    private int minLives;

    private boolean enableWithdrawCommand;
    private boolean dropLifeOnDeath;
    private boolean enableReviveBook;
    private String reviveBookBaseItem;
    private int reviveBookLifeItemsRequired;

    // Elimination
    private String eliminationAction;
    private boolean kickOnElimination;
    private String kickMessage;
    private boolean runCommandsOnElimination;
    private java.util.List<String> eliminationCommands;

    // Nametag
    private String heartSymbol;
    private String heartColor;
    private String heartLowColor;
    private int updateIntervalTicks;

    // Messages
    private String prefix;
    private String msgLivesCheckSelf;
    private String msgLivesCheckOther;
    private String msgLivesSet;
    private String msgLivesAdd;
    private String msgLivesRemove;
    private String msgLivesReset;
    private String msgLifeLost;
    private String msgEliminated;
    private String msgNoPermission;
    private String msgPlayerNotFound;
    private String msgInvalidNumber;
    private String msgLivesMaxed;
    private String msgLivesAlreadyZero;

    public ConfigManager(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        FileConfiguration cfg = plugin.getConfig();

        maxLives = Math.max(1, Math.min(64, cfg.getInt("max-lives", 3)));
        defaultLives = Math.max(1, Math.min(maxLives, cfg.getInt("default-lives", 3)));
        // Zero must remain reachable for elimination to work.
        minLives = 0;

        enableWithdrawCommand = cfg.getBoolean("enable-withdraw-command", true);
        dropLifeOnDeath = cfg.getBoolean("drop-life-on-death", true);
        enableReviveBook = cfg.getBoolean("enable-revive-book", true);
        
        reviveBookBaseItem = cfg.getString("revive-book-recipe.base-item", "BOOK");
        reviveBookLifeItemsRequired = Math.max(0, Math.min(8, cfg.getInt("revive-book-recipe.life-items-required", 3)));

        eliminationAction = cfg.getString("elimination.elimination-action", "DEATH_BOX");
        kickOnElimination = cfg.getBoolean("elimination.kick-on-elimination", false);
        kickMessage = color(cfg.getString("elimination.kick-message", "&4You have been eliminated!"));
        runCommandsOnElimination = cfg.getBoolean("elimination.run-commands", false);
        eliminationCommands = cfg.getStringList("elimination.commands");

        heartSymbol = cfg.getString("nametag.heart-symbol", "❤");
        heartColor = color(cfg.getString("nametag.heart-color", "&c"));
        heartLowColor = color(cfg.getString("nametag.heart-low-color", "&4"));
        updateIntervalTicks = cfg.getInt("nametag.update-interval-ticks", 20);

        prefix = color(cfg.getString("messages.prefix", "&8[&cThreeLives&8] &r"));
        msgLivesCheckSelf = color(cfg.getString("messages.lives-check-self", "&7You have &c{lives} &7life/lives remaining."));
        msgLivesCheckOther = color(cfg.getString("messages.lives-check-other", "&7{player} has &c{lives} &7life/lives remaining."));
        msgLivesSet = color(cfg.getString("messages.lives-set", "&7{player}'s lives have been set to &c{lives}&7."));
        msgLivesAdd = color(cfg.getString("messages.lives-add", "&7Added &a{amount} &7life/lives to &c{player}&7. They now have &c{lives}&7."));
        msgLivesRemove = color(cfg.getString("messages.lives-remove", "&7Removed &c{amount} &7life/lives from &c{player}&7. They now have &c{lives}&7."));
        msgLivesReset = color(cfg.getString("messages.lives-reset", "&7{player}'s lives have been reset to &c{lives}&7."));
        msgLifeLost = color(cfg.getString("messages.life-lost", "&cYou lost a life! You have &4{lives} &clives remaining."));
        msgEliminated = color(cfg.getString("messages.eliminated", "&4You have been eliminated! You have no lives remaining."));
        msgNoPermission = color(cfg.getString("messages.no-permission", "&cYou do not have permission to do that."));
        msgPlayerNotFound = color(cfg.getString("messages.player-not-found", "&cPlayer &4{player} &cnot found."));
        msgInvalidNumber = color(cfg.getString("messages.invalid-number", "&cPlease provide a valid number."));
        msgLivesMaxed = color(cfg.getString("messages.lives-maxed", "&cThat player is already at maximum lives ({max})."));
        msgLivesAlreadyZero = color(cfg.getString("messages.lives-already-zero", "&cThat player already has 0 lives."));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Builds the colored hearts string for the given lives count.
     * E.g. 2 lives → "❤❤" (with heart-color) or "❤" (with low-color) for 1
     */
    public String buildHeartsString(int lives) {
        if (lives <= 0) return ChatColor.DARK_GRAY + "✗"; // dead symbol
        String color = (lives == 1) ? heartLowColor : heartColor;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lives; i++) {
            sb.append(color).append(heartSymbol);
        }
        return sb.toString();
    }

    // ── Getters ──────────────────────────────────────────────────────────────────

    public int getDefaultLives() { return defaultLives; }
    public int getMaxLives() { return maxLives; }
    public int getMinLives() { return minLives; }

    public String getEliminationAction() {
        return eliminationAction;
    }

    public String getBanReason() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("elimination.ban-reason", "&4You have been eliminated! You have no lives remaining."));
    }

    public boolean isKickOnElimination() { return kickOnElimination; }
    public String getKickMessage() { return kickMessage; }
    public boolean isRunCommandsOnElimination() { return runCommandsOnElimination; }
    public java.util.List<String> getEliminationCommands() { return eliminationCommands; }
    public int getUpdateIntervalTicks() { return updateIntervalTicks; }
    public String getPrefix() { return prefix; }

    public String getMsgLivesCheckSelf() { return msgLivesCheckSelf; }
    public String getMsgLivesCheckOther() { return msgLivesCheckOther; }
    public String getMsgLivesSet() { return msgLivesSet; }
    public String getMsgLivesAdd() { return msgLivesAdd; }
    public String getMsgLivesRemove() { return msgLivesRemove; }
    public String getMsgLivesReset() { return msgLivesReset; }
    public String getMsgLifeLost() { return msgLifeLost; }
    public String getMsgEliminated() { return msgEliminated; }
    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgPlayerNotFound() { return msgPlayerNotFound; }
    public String getMsgInvalidNumber() { return msgInvalidNumber; }
    public String getMsgLivesMaxed() { return msgLivesMaxed; }
    public String getMsgLivesAlreadyZero() { return msgLivesAlreadyZero; }

    public String getMsgWithdrawSuccess() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.withdraw-success", ""));
    }

    public String getMsgWithdrawNotEnough() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.withdraw-not-enough", ""));
    }

    public String getMsgRedeemSuccess() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.redeem-success", ""));
    }

    public String getMsgRedeemMaxed() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.redeem-maxed", ""));
    }
    
    public boolean isEnableWithdrawCommand() {
        return enableWithdrawCommand;
    }
    
    public boolean isDropLifeOnDeath() {
        return dropLifeOnDeath;
    }
    
    public boolean isEnableReviveBook() {
        return enableReviveBook;
    }
    
    public String getReviveBookBaseItem() {
        return reviveBookBaseItem;
    }
    
    public int getReviveBookLifeItemsRequired() {
        return reviveBookLifeItemsRequired;
    }
}
