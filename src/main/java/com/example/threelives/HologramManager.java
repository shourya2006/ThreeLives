package com.example.threelives;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the heart-hologram displayed beneath every online player's nametag.
 *
 * Implementation: An invisible, silent, invulnerable ArmorStand is summoned
 * just below the player's head. Its custom name shows the hearts (e.g. ❤❤❤).
 * The stand follows the player via a repeating task.
 *
 * This approach works on all Paper 1.20+ servers without packet libraries.
 */
public class HologramManager {

    // Y offset from player's feet to place the armor stand name tag.
    // ~1.9 = just above the player head (nameplate is at ~2.0), 1.75 puts hearts just below.
    private static final double OFFSET_Y = 1.75;

    private final ThreeLivesPlugin plugin;
    /** Maps player UUID → their ArmorStand hologram entity */
    private final Map<UUID, ArmorStand> holograms = new HashMap<>();
    private BukkitTask updateTask;

    public HologramManager(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Task ─────────────────────────────────────────────────────────────────────

    public void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        int interval = plugin.getConfigManager().getUpdateIntervalTicks();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, interval);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ArmorStand stand = holograms.get(player.getUniqueId());
            if (stand == null || stand.isDead() || !stand.isValid()) {
                // (Re-)spawn the stand
                stand = spawnStand(player);
                holograms.put(player.getUniqueId(), stand);
            }
            // Update position to follow player
            Location target = player.getLocation().clone().add(0, OFFSET_Y, 0);
            stand.teleport(target);
            // Update heart display
            refreshHearts(player, stand);
        }
    }

    // ── Spawning ─────────────────────────────────────────────────────────────────

    private ArmorStand spawnStand(Player player) {
        Location loc = player.getLocation().clone().add(0, OFFSET_Y, 0);
        // Use world.spawn() to avoid triggering CreatureSpawnEvent cancellations
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        configureStand(stand, player);
        return stand;
    }

    private void configureStand(ArmorStand stand, Player player) {
        stand.setVisible(false);               // Invisible armor stand
        stand.setGravity(false);               // No falling
        stand.setMarker(true);                 // No hitbox (marker)
        stand.setSmall(true);                  // Small stand
        stand.setCustomNameVisible(true);      // Always show the name tag
        stand.setSilent(true);
        stand.setInvulnerable(true);
        stand.setCanPickupItems(false);
        stand.setPersistent(false);            // Won't be saved by the server

        // Tag it so we can identify it on reload
        stand.addScoreboardTag("threelives_hologram");
        stand.addScoreboardTag("threelives_" + player.getUniqueId());

        // Set initial heart display
        refreshHearts(player, stand);
    }

    private void refreshHearts(Player player, ArmorStand stand) {
        int lives = plugin.getLivesDataManager().getLives(player);
        String hearts = plugin.getConfigManager().buildHeartsString(lives);
        stand.setCustomName(hearts);
    }

    // ── Player join/leave ─────────────────────────────────────────────────────────

    public void onPlayerJoin(Player player) {
        // Will be created on the next tick – no need to do anything here.
    }

    public void onPlayerQuit(Player player) {
        removeHologram(player.getUniqueId());
    }

    /** Force-refresh a single player's hearts immediately (called after lives change). */
    public void refreshPlayer(Player player) {
        ArmorStand stand = holograms.get(player.getUniqueId());
        if (stand != null && stand.isValid()) {
            refreshHearts(player, stand);
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────────

    public void removeHologram(UUID uuid) {
        ArmorStand stand = holograms.remove(uuid);
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    /**
     * Remove all tracked holograms (called on plugin disable / reload).
     * Also cleans up any orphaned stands tagged with threelives_hologram.
     */
    public void removeAllHolograms() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        // Remove tracked
        for (ArmorStand stand : holograms.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        holograms.clear();

        // Sweep all worlds for orphaned stands
        Bukkit.getWorlds().forEach(world ->
                world.getEntitiesByClass(ArmorStand.class).stream()
                        .filter(e -> e.getScoreboardTags().contains("threelives_hologram"))
                        .forEach(Entity::remove)
        );
    }
}
