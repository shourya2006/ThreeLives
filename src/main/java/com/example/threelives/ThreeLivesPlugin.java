package com.example.threelives;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ThreeLivesPlugin extends JavaPlugin {

    private static ThreeLivesPlugin instance;
    private ConfigManager configManager;
    private LivesDataManager livesDataManager;
    private NametagManager nametagManager;
    private WithdrawManager withdrawManager;
    private BoxManager boxManager;
    private ReviveManager reviveManager;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Init managers
        configManager = new ConfigManager(this);
        livesDataManager = new LivesDataManager(this);
        nametagManager = new NametagManager(this);
        withdrawManager = new WithdrawManager(this);
        boxManager = new BoxManager(this);
        reviveManager = new ReviveManager(this);

        // Register listeners
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(reviveManager, this);

        // Register commands
        getCommand("threelives").setExecutor(new ThreeLivesCommand(this));
        
        LivesCommand livesCommand = new LivesCommand(this);
        getCommand("lives").setExecutor(livesCommand);
        getCommand("lives").setTabCompleter(livesCommand);
        getCommand("setdeathbox").setExecutor(new BoxCommand(this));
        getCommand("setlives").setExecutor(livesCommand);
        getCommand("setlives").setTabCompleter(livesCommand);
        getCommand("resetlives").setExecutor(livesCommand);
        getCommand("resetlives").setTabCompleter(livesCommand);
        getCommand("addlives").setExecutor(livesCommand);
        getCommand("addlives").setTabCompleter(livesCommand);
        getCommand("removelives").setExecutor(livesCommand);
        getCommand("removelives").setTabCompleter(livesCommand);
        getCommand("withdraw").setExecutor(livesCommand);
        getCommand("withdraw").setTabCompleter(livesCommand);

        // Setup nametags for any players already online (reload scenario)
        for (Player player : Bukkit.getOnlinePlayers()) {
            nametagManager.setupPlayer(player);
        }

        getLogger().info("ThreeLives plugin enabled! ❤❤❤");
    }

    @Override
    public void onDisable() {
        // Remove all scoreboard teams cleanly
        if (nametagManager != null) {
            nametagManager.removeAllTeams();
        }
        // Save lives data
        if (livesDataManager != null) {
            livesDataManager.saveAll();
        }
        getLogger().info("ThreeLives plugin disabled.");
    }

    // ── Reload ──────────────────────────────────────────────────────────────────

    public void reload() {
        reloadConfig();
        configManager.reload();
        livesDataManager.reload();
        if (reviveManager != null) {
            reviveManager.reloadRecipe();
        }
        
        // Rebuild all nametags with fresh config values
        nametagManager.removeAllTeams();
        for (Player player : Bukkit.getOnlinePlayers()) {
            nametagManager.setupPlayer(player);
        }
    }

    // ── Getters ─────────────────────────────────────────────────────────────────

    public static ThreeLivesPlugin getInstance() {
        return instance;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LivesDataManager getLivesDataManager() {
        return livesDataManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public WithdrawManager getWithdrawManager() {
        return withdrawManager;
    }

    public BoxManager getBoxManager() {
        return boxManager;
    }
}
