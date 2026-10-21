package com.example.threelives;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles persistent storage of each player's lives in a YAML flat-file (data.yml).
 */
public class LivesDataManager {

    private final ThreeLivesPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    /** In-memory cache: UUID → lives */
    private final Map<UUID, Integer> livesCache = new HashMap<>();

    public LivesDataManager(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // ── Persistence ──────────────────────────────────────────────────────────────

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load all entries into cache
        livesCache.clear();
        if (dataConfig.isConfigurationSection("lives")) {
            for (String key : dataConfig.getConfigurationSection("lives").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int lives = dataConfig.getInt("lives." + key,
                            plugin.getConfigManager().getDefaultLives());
                    livesCache.put(uuid, lives);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void reload() {
        load();
    }
    
    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    public void saveAll() {
        for (Map.Entry<UUID, Integer> entry : livesCache.entrySet()) {
            dataConfig.set("lives." + entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    /** Save a single player's data immediately. */
    private void save(UUID uuid) {
        dataConfig.set("lives." + uuid.toString(), livesCache.get(uuid));
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    // ── API ───────────────────────────────────────────────────────────────────────

    /**
     * Returns the lives of a player, initialising to default if not present.
     */
    public int getLives(UUID uuid) {
        return livesCache.computeIfAbsent(uuid,
                k -> plugin.getConfigManager().getDefaultLives());
    }

    public int getLives(Player player) {
        return getLives(player.getUniqueId());
    }

    /**
     * Directly set a player's lives (clamped to [0, max]).
     */
    public void setLives(UUID uuid, int lives) {
        int max = plugin.getConfigManager().getMaxLives();
        int min = plugin.getConfigManager().getMinLives();
        lives = Math.max(min, Math.min(max, lives));
        livesCache.put(uuid, lives);
        save(uuid);
    }

    public void setLives(Player player, int lives) {
        setLives(player.getUniqueId(), lives);
    }

    /**
     * Add lives (won't exceed max).
     */
    public int addLives(UUID uuid, int amount) {
        int current = getLives(uuid);
        int newLives = (int) Math.min((long) current + Math.max(0, amount), plugin.getConfigManager().getMaxLives());
        setLives(uuid, newLives);
        return newLives;
    }

    /**
     * Remove lives (won't go below 0).
     */
    public int removeLives(UUID uuid, int amount) {
        int current = getLives(uuid);
        int newLives = (int) Math.max((long) current - Math.max(0, amount), 0);
        setLives(uuid, newLives);
        return newLives;
    }

    /**
     * Reset a player back to the default lives count.
     */
    public int resetLives(UUID uuid) {
        int def = plugin.getConfigManager().getDefaultLives();
        setLives(uuid, def);
        return def;
    }

    /**
     * Whether a player has been eliminated (0 lives).
     */
    public boolean isEliminated(UUID uuid) {
        return getLives(uuid) <= 0;
    }
}
