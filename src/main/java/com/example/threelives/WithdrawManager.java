package com.example.threelives;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the creation and validation of the Life Item (Nether Star).
 */
public class WithdrawManager {

    private final ThreeLivesPlugin plugin;
    private final NamespacedKey lifeKey;

    public WithdrawManager(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
        this.lifeKey = new NamespacedKey(plugin, "life_item");
    }

    /**
     * Creates a custom Nether Star representing a single life.
     */
    public ItemStack createLifeItem(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Display Name: "&c&lLife Item"
            meta.displayName(Component.text("Life Item")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            // Lore
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Right-click to redeem 1 life!")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            
            // Custom model data for hardcore heart texture
            meta.setCustomModelData(1);

            // PersistentDataContainer tag to prevent spoofing
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(lifeKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Checks if an item is a valid Life Item.
     */
    public boolean isLifeItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return Byte.valueOf((byte) 1).equals(pdc.get(lifeKey, PersistentDataType.BYTE));
    }
}
