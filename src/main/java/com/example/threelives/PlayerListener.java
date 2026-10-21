package com.example.threelives;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Date;

/**
 * Handles all player-related events: join, quit, death, respawn, and interactions.
 */
public class PlayerListener implements Listener {

    private final ThreeLivesPlugin plugin;

    public PlayerListener(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
    }


    /** Apply the same elimination/release rules after administrative life changes. */
    public void synchronizeLives(Player player, int previousLives) {
        int currentLives = plugin.getLivesDataManager().getLives(player);
        plugin.getNametagManager().refreshPlayer(player);
        if (currentLives <= 0 && previousLives > 0) {
            handleElimination(player, plugin.getConfigManager());
        } else if (currentLives > 0 && previousLives <= 0
                && plugin.getBoxManager().isInside(player.getLocation())) {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getTo() == null || plugin.getLivesDataManager().getLives(event.getPlayer()) > 0
                || !"DEATH_BOX".equalsIgnoreCase(plugin.getConfigManager().getEliminationAction())) return;
        org.bukkit.Location box = plugin.getBoxManager().getGlobalBoxLocation();
        if (box != null && !plugin.getBoxManager().isInside(event.getTo())) {
            box.setYaw(event.getTo().getYaw());
            box.setPitch(event.getTo().getPitch());
            event.setTo(box);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.getBoxManager().isProtected(block));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> plugin.getBoxManager().isProtected(block)
                || plugin.getBoxManager().isProtected(block.getRelative(event.getDirection())))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> plugin.getBoxManager().isProtected(block)
                || plugin.getBoxManager().isProtected(block.getRelative(event.getDirection().getOppositeFace())))) event.setCancelled(true);
    }

    // ── Join ──────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Ensure a lives entry exists (initialises new players)
        int lives = plugin.getLivesDataManager().getLives(player);
        // Set up the TextDisplay nametag
        plugin.getNametagManager().setupPlayer(player);
        
        ConfigManager cfg = plugin.getConfigManager();
        
        if (lives > 0) {
            // If they have lives, but are logging in inside the death box (revived while offline)
            org.bukkit.Location boxLoc = plugin.getBoxManager().getGlobalBoxLocation();
            if (boxLoc != null && player.getLocation().getWorld().equals(boxLoc.getWorld())) {
                if (player.getLocation().distanceSquared(boxLoc) < 100) { // Within 10 blocks of center
                    player.teleport(player.getWorld().getSpawnLocation());
                }
            }
        } else {
            // Player has 0 lives on join
            String action = cfg.getEliminationAction();
            if ("BAN".equalsIgnoreCase(action)) {
                String reason = cfg.getBanReason();
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), reason, null, "ThreeLives");
                player.kickPlayer(reason);
            } else if ("DEATH_BOX".equalsIgnoreCase(action)) {
                org.bukkit.Location boxLoc = plugin.getBoxManager().getGlobalBoxLocation();
                if (boxLoc != null && (!player.getLocation().getWorld().equals(boxLoc.getWorld()) || player.getLocation().distanceSquared(boxLoc) >= 100)) {
                    player.teleport(boxLoc);
                }
            }
        }
    }

    // ── Quit ──────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getNametagManager().removePlayer(event.getPlayer());
    }

    // ── Death ─────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathEarly(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 1. Temporarily remove player from team so client/plugins don't append the suffix in chat
        plugin.getNametagManager().removePlayer(player);

        // 2. Clean the already-generated death message of any hearts (ignoring color codes)
        net.kyori.adventure.text.Component deathMsgComp = event.deathMessage();
        if (deathMsgComp != null) {
            String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(deathMsgComp);
            // Regex removes optional space + any sequence of color codes and hearts/crosses (including custom unicode)
            legacy = legacy.replaceAll(" ?(§[0-9a-fA-Fk-oK-OrR])*([❤✗\\uE000\\uE001](§[0-9a-fA-Fk-oK-OrR])*)+", "");
            event.deathMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacy));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Bypass permission — admins won't lose lives
        if (player.hasPermission("threelives.bypass")) return;

        LivesDataManager data = plugin.getLivesDataManager();
        ConfigManager cfg = plugin.getConfigManager();

        int currentLives = data.getLives(player);
        if (currentLives <= 0) return; // Already at 0

        int newLives = data.removeLives(player.getUniqueId(), 1);

        // Drop a life item at death location if configured
        if (cfg.isDropLifeOnDeath()) {
            ItemStack lifeItem = plugin.getWithdrawManager().createLifeItem(1);
            player.getWorld().dropItemNaturally(player.getLocation(), lifeItem);
        }

        // Refresh nametag hearts immediately (1 tick later so death is processed first)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getNametagManager().refreshPlayer(player);
            }
        }, 1L);

        if (newLives <= 0) {
            // Player is now eliminated
            player.sendMessage(cfg.getPrefix() + cfg.getMsgEliminated());
            handleElimination(player, cfg);
        } else {
            // Notify player of remaining lives (shown after respawn screen)
            String msg = cfg.getPrefix() + cfg.getMsgLifeLost()
                    .replace("{lives}", String.valueOf(newLives));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(msg);
                }
            }, 20L);
        }
    }

    // ── Respawn ───────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // If eliminated, and action is DEATH_BOX, force them to respawn inside the global death box
        if (plugin.getLivesDataManager().getLives(player) <= 0) {
            if ("DEATH_BOX".equalsIgnoreCase(plugin.getConfigManager().getEliminationAction())) {
                org.bukkit.Location boxLoc = plugin.getBoxManager().getGlobalBoxLocation();
                if (boxLoc != null) {
                    event.setRespawnLocation(boxLoc);
                }
            }
        }
        
        // Re-apply nametag after respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getNametagManager().refreshPlayer(player);
            }
        }, 2L);
    }

    // ── Elimination ───────────────────────────────────────────────────────────────

    private void handleElimination(Player player, ConfigManager cfg) {
        String action = cfg.getEliminationAction();
        
        if ("DEATH_BOX".equalsIgnoreCase(action)) {
            // Teleport to global death box if it's set
            org.bukkit.Location boxLoc = plugin.getBoxManager().getGlobalBoxLocation();
            if (boxLoc != null) {
                player.teleport(boxLoc);
            } else {
                player.sendMessage(cfg.getPrefix() + "§cWarning: The global death box has not been set by an admin! (/setdeathbox)");
            }
        } else if ("BAN".equalsIgnoreCase(action)) {
            // Ban the player
            String reason = cfg.getBanReason();
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), reason, null, "ThreeLives");
            player.kickPlayer(reason);
        }
        
        if (cfg.isRunCommandsOnElimination()) {
            for (String cmd : cfg.getEliminationCommands()) {
                String finalCmd = cmd.replace("{player}", player.getName());
                Bukkit.getScheduler().runTask(plugin,
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
            }
        }
    }
    
    // ── Interaction (Redeem Life) ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (plugin.getWithdrawManager().isLifeItem(item)) {
            event.setCancelled(true);
            
            ConfigManager cfg = plugin.getConfigManager();
            LivesDataManager data = plugin.getLivesDataManager();
            
            int currentLives = data.getLives(player);
            if (currentLives >= cfg.getMaxLives()) {
                player.sendMessage(cfg.getPrefix() + cfg.getMsgRedeemMaxed().replace("{max}", String.valueOf(cfg.getMaxLives())));
                return;
            }
            
            // Consume 1 item
            item.setAmount(item.getAmount() - 1);
            
            // Add 1 life
            int newLives = data.addLives(player.getUniqueId(), 1);
            synchronizeLives(player, currentLives);
            
            player.sendMessage(cfg.getPrefix() + cfg.getMsgRedeemSuccess().replace("{lives}", String.valueOf(newLives)));
        }
    }

    // ── Box Protection ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (plugin.getBoxManager().isProtected(event.getBlock())) {
            Player player = event.getPlayer();
            if (player.hasPermission("threelives.admin") && player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                // Allow admin to break it
                plugin.getBoxManager().unprotectBlock(event.getBlock());
            } else {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis is an unbreakable death box!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.getBoxManager().isProtected(block));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (plugin.getLivesDataManager().getLives(event.getPlayer()) <= 0) {
            if ("DEATH_BOX".equalsIgnoreCase(plugin.getConfigManager().getEliminationAction())) {
                // Only allow teleports if it's the plugin respawning them or a plugin teleporting them into their box
                // If they are eliminated, they cannot teleport out of their box via ender pearls or chorus fruits
                if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL || 
                    event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT ||
                    event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix() + "§cYou cannot teleport while eliminated!");
                }
            }
        }
    }
}
