package com.example.threelives;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Displays hearts right next to the player's nametag using Scoreboard Teams.
 *
 * We set the player's PlayerListName to explicitly prevent the team suffix
 * from bleeding into the TAB list!
 */
public class NametagManager {

    private static final String TEAM_PREFIX = "tl_";

    private final ThreeLivesPlugin plugin;
    private final Scoreboard scoreboard;
    private final Map<UUID, String> playerTeamNames = new HashMap<>();

    public NametagManager(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        cleanupOldTeams();
    }

    public void setupPlayer(Player player) {
        removePlayer(player);

        String teamName = buildTeamName(player.getUniqueId());
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        } else {
            for (String entry : team.getEntries()) {
                team.removeEntry(entry);
            }
        }

        team.addEntry(player.getName());
        playerTeamNames.put(player.getUniqueId(), teamName);

        // Explicitly set the Tab List name so the Scoreboard Team suffix doesn't bleed into it!
        player.playerListName(Component.text(player.getName()));

        applyHearts(team, player);
    }

    public void removePlayer(Player player) {
        String teamName = playerTeamNames.remove(player.getUniqueId());
        if (teamName == null) return;
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }

    public void refreshPlayer(Player player) {
        String teamName = playerTeamNames.get(player.getUniqueId());
        if (teamName == null) {
            setupPlayer(player);
            return;
        }
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            setupPlayer(player);
            return;
        }
        applyHearts(team, player);
    }

    private void applyHearts(Team team, Player player) {
        int lives = plugin.getLivesDataManager().getLives(player);
        Component hearts = buildHeartsComponent(lives);

        // Space + Hearts -> Appears directly at the side of the nametag
        Component suffix = Component.text(" ").append(hearts);
        team.suffix(suffix);
        team.color(NamedTextColor.WHITE);
    }

    private Component buildHeartsComponent(int lives) {
        int maxLives = plugin.getConfigManager().getMaxLives();
        Component hearts = Component.empty();

        for (int i = 1; i <= maxLives; i++) {
            if (i <= lives) {
                // Active life (Custom texture \uE000 colored white to retain image color)
                hearts = hearts.append(Component.text("\uE000").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
            } else {
                // Lost life (Custom texture \uE001 colored white to retain image color)
                hearts = hearts.append(Component.text("\uE001").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
            }
        }
        
        return hearts;
    }

    public void removeAllTeams() {
        for (String teamName : playerTeamNames.values()) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
        playerTeamNames.clear();
        cleanupOldTeams();
    }

    private void cleanupOldTeams() {
        scoreboard.getTeams().stream()
                .filter(t -> t.getName().startsWith(TEAM_PREFIX))
                .forEach(Team::unregister);
        
        // Also cleanup TextDisplays from previous version just in case
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity e : w.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                if (e.getVehicle() instanceof Player) {
                    e.remove();
                }
            }
        }
    }

    private String buildTeamName(UUID uuid) {
        return TEAM_PREFIX + uuid.toString().replace("-", "").substring(0, 12);
    }
}
