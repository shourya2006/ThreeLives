package com.example.threelives;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Crafts genuine revive books and binds each menu to its owner and target UUIDs. */
public class ReviveManager implements Listener {
    private final ThreeLivesPlugin plugin;
    private final NamespacedKey reviveKey;

    private static final class ReviveMenu implements InventoryHolder {
        final UUID owner;
        final int page;
        final Map<Integer, UUID> targets = new HashMap<>();
        final Inventory inventory;
        ReviveMenu(UUID owner, int page) {
            this.owner = owner;
            this.page = page;
            this.inventory = Bukkit.createInventory(this, 54,
                    Component.text("Revive a Player").color(NamedTextColor.DARK_RED));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    public ReviveManager(ThreeLivesPlugin plugin) {
        this.plugin = plugin;
        this.reviveKey = new NamespacedKey(plugin, "revive_book");
        if (plugin.getConfigManager().isEnableReviveBook()) registerRecipe();
    }

    private boolean isReviveBook(ItemStack item) {
        return item != null && item.getType() == Material.ENCHANTED_BOOK && item.hasItemMeta()
                && Byte.valueOf((byte) 1).equals(item.getItemMeta().getPersistentDataContainer()
                .get(reviveKey, PersistentDataType.BYTE));
    }

    private ItemStack heldBook(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isReviveBook(main)) return main;
        ItemStack off = player.getInventory().getItemInOffHand();
        return isReviveBook(off) ? off : null;
    }

    private void registerRecipe() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.displayName(Component.text("Revive Book").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        meta.lore(List.of(Component.text("Right-click to revive an eliminated player!").color(NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(reviveKey, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, "revive_book_recipe"), book);
        Material base = Material.matchMaterial(plugin.getConfigManager().getReviveBookBaseItem());
        if (base == null || !base.isItem() || base.isAir() || base == Material.NETHER_STAR) base = Material.BOOK;
        recipe.addIngredient(base);
        int required = plugin.getConfigManager().getReviveBookLifeItemsRequired();
        if (required > 0) recipe.addIngredient(required, Material.NETHER_STAR);
        Bukkit.addRecipe(recipe);
    }

    public void reloadRecipe() {
        Bukkit.removeRecipe(new NamespacedKey(plugin, "revive_book_recipe"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof ReviveMenu) player.closeInventory();
        }
        if (plugin.getConfigManager().isEnableReviveBook()) registerRecipe();
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!isReviveBook(event.getInventory().getResult())) return;
        if (!plugin.getConfigManager().isEnableReviveBook()) { event.getInventory().setResult(null); return; }
        int count = 0;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null || item.getType() != Material.NETHER_STAR) continue;
            if (!plugin.getWithdrawManager().isLifeItem(item)) { event.getInventory().setResult(null); return; }
            count++;
        }
        if (count != plugin.getConfigManager().getReviveBookLifeItemsRequired()) event.getInventory().setResult(null);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isEnableReviveBook()
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !isReviveBook(event.getItem())) return;
        event.setCancelled(true);
        if (plugin.getLivesDataManager().isEliminated(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix() + "§cAn eliminated player cannot use a Revive Book.");
            return;
        }
        openReviveMenu(event.getPlayer(), 0);
    }

    private void openReviveMenu(Player player, int requestedPage) {
        List<OfflinePlayer> targets = new ArrayList<>();
        var section = plugin.getLivesDataManager().getDataConfig().getConfigurationSection("lives");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (section.getInt(key) > 0) continue;
                try {
                    OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(key));
                    if (target.getName() != null) targets.add(target);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        targets.sort(java.util.Comparator.comparing(p -> p.getName(), String.CASE_INSENSITIVE_ORDER));
        if (targets.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cThere are no eliminated players to revive.");
            return;
        }
        int page = Math.max(0, Math.min(requestedPage, (targets.size() - 1) / 45));
        ReviveMenu menu = new ReviveMenu(player.getUniqueId(), page);
        for (int index = page * 45; index < Math.min(targets.size(), (page + 1) * 45); index++) {
            OfflinePlayer target = targets.get(index);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName()).color(NamedTextColor.RED));
            meta.lore(List.of(Component.text("Click to restore their lives.").color(NamedTextColor.GREEN)));
            skull.setItemMeta(meta);
            int slot = index - page * 45;
            menu.inventory.setItem(slot, skull);
            menu.targets.put(slot, target.getUniqueId());
        }
        if (page > 0) menu.inventory.setItem(45, arrow("Previous page"));
        if ((page + 1) * 45 < targets.size()) menu.inventory.setItem(53, arrow("Next page"));
        player.openInventory(menu.inventory);
    }

    private ItemStack arrow(String label) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label).color(NamedTextColor.GOLD));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ReviveMenu) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ReviveMenu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !menu.owner.equals(player.getUniqueId())
                || !plugin.getConfigManager().isEnableReviveBook()
                || plugin.getLivesDataManager().isEliminated(player.getUniqueId())) return;
        if (event.getClickedInventory() != menu.inventory || !event.isLeftClick() || event.isShiftClick()) return;
        ItemStack book = heldBook(player);
        if (book == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou must still hold a Revive Book.");
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            return;
        }
        int slot = event.getRawSlot();
        if ((slot == 45 || slot == 53) && menu.inventory.getItem(slot) != null) {
            Bukkit.getScheduler().runTask(plugin, () -> openReviveMenu(player, menu.page + (slot == 45 ? -1 : 1)));
            return;
        }
        UUID targetId = menu.targets.get(slot);
        if (targetId == null || !plugin.getLivesDataManager().isEliminated(targetId)) return;
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        // Consume the verified book before changing any life state. All operations run on the server thread.
        boolean mainHand = isReviveBook(player.getInventory().getItemInMainHand());
        book.setAmount(book.getAmount() - 1);
        if (mainHand) {
            player.getInventory().setItemInMainHand(book);
        } else {
            player.getInventory().setItemInOffHand(book);
        }
        plugin.getLivesDataManager().resetLives(targetId);
        var bans = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
        var entry = bans.getBanEntry(target.getName());
        if (entry != null && "ThreeLives".equals(entry.getSource())) bans.pardon(target.getName());
        if (target.getPlayer() != null) {
            Player revived = target.getPlayer();
            plugin.getPlayerListener().synchronizeLives(revived, 0);
            revived.sendMessage(plugin.getConfigManager().getPrefix() + "§aYou have been revived by " + player.getName() + "!");
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        Bukkit.broadcast(Component.text(target.getName() + " has been revived by " + player.getName() + "!").color(NamedTextColor.GREEN));
    }
}
