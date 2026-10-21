package com.example.threelives;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles the creation and tracking of the global 5x5 unbreakable death box.
 */
public class BoxManager {

    private final ThreeLivesPlugin plugin;
    private final File boxesFile;
    private FileConfiguration boxesConfig;
    
    // Store locations of protected blocks in format "world,x,y,z"
    private final Set<String> protectedBlocks = new HashSet<>();
    
    private Location globalBoxLocation;

    public BoxManager(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
        this.boxesFile = new File(plugin.getDataFolder(), "boxes.yml");
        load();
    }

    public void load() {
        if (!boxesFile.exists()) {
            try {
                boxesFile.getParentFile().mkdirs();
                boxesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create boxes.yml!");
            }
        }
        boxesConfig = YamlConfiguration.loadConfiguration(boxesFile);
        
        protectedBlocks.clear();
        List<String> list = boxesConfig.getStringList("protected-blocks");
        protectedBlocks.addAll(list);
        
        globalBoxLocation = boxesConfig.getLocation("global-box");
    }

    public void save() {
        boxesConfig.set("protected-blocks", List.copyOf(protectedBlocks));
        boxesConfig.set("global-box", globalBoxLocation);
        
        try {
            boxesConfig.save(boxesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save boxes.yml!");
        }
    }

    public boolean isProtected(Block block) {
        return protectedBlocks.contains(formatLoc(block.getLocation()));
    }
    
    public void unprotectBlock(Block block) {
        if (protectedBlocks.remove(formatLoc(block.getLocation()))) {
            save();
        }
    }
    
    public Location getGlobalBoxLocation() {
        return globalBoxLocation == null ? null : globalBoxLocation.clone();
    }


    public boolean isInside(Location location) {
        Location center = globalBoxLocation;
        return center != null && center.getWorld().equals(location.getWorld())
                && location.getX() >= center.getX() - 1.5 && location.getX() < center.getX() + 1.5
                && location.getZ() >= center.getZ() - 1.5 && location.getZ() < center.getZ() + 1.5
                && location.getY() >= center.getY() && location.getY() < center.getY() + 3;
    }

    public void setGlobalBoxLocation(Location center) {
        World w = center.getWorld();
        if (w == null) return;
        
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        
        // Ensure center location is center of block for respawning safely, and +1 Y so they stand on the floor
        globalBoxLocation = new Location(w, cx + 0.5, cy + 1.0, cz + 0.5, center.getYaw(), center.getPitch());

        // Generate 5x5 box
        // x and z from -2 to 2 (5 blocks wide)
        // y from 0 to 4 (5 blocks tall)
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    
                    Block block = w.getBlockAt(cx + x, cy + y, cz + z);

                    // Floor (y=0) and Ceiling (y=4) are solid Obsidian
                    if (y == 0 || y == 4) {
                        block.setType(Material.OBSIDIAN);
                        protectedBlocks.add(formatLoc(block.getLocation()));
                    } 
                    // Walls (y=1, 2, 3) are Iron bars ONLY on the edges
                    else if (x == -2 || x == 2 || z == -2 || z == 2) {
                        block.setType(Material.IRON_BARS);
                        protectedBlocks.add(formatLoc(block.getLocation()));
                    }
                    // Inside the box (air)
                    else {
                        if (block.getType() != Material.AIR) {
                            block.setType(Material.AIR);
                        }
                        // Remove from protected if it was previously protected
                        protectedBlocks.remove(formatLoc(block.getLocation()));
                    }
                }
            }
        }
        
        save();
    }

    private String formatLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
