package com.autotrash.gui;

import com.autotrash.AutoTrash;
import com.autotrash.util.MaterialUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class BrowseAllGui implements GuiHolder {

    private static final int ITEMS_PER_PAGE = 45;

    private final AutoTrash plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Material> sortedMaterials;
    private final Map<Material, Integer> trashCounts;
    private int currentPage = 0;

    private BrowseAllGui(AutoTrash plugin, Player player, Map<Material, Integer> trashCounts) {
        this.plugin = plugin;
        this.player = player;
        this.trashCounts = trashCounts;
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text("Browse All Items", NamedTextColor.DARK_GREEN, TextDecoration.BOLD));

        // Sort: most trashed first, then alphabetically for items with no trash count
        List<Material> all = new ArrayList<>(MaterialUtil.getPickupMaterials());
        all.sort(Comparator.<Material, Integer>comparing(m -> trashCounts.getOrDefault(m, 0))
                .reversed()
                .thenComparing(Material::name));
        this.sortedMaterials = all;
    }

    private int totalPages() {
        return Math.max(1, (sortedMaterials.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    private List<Material> currentPageItems() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, sortedMaterials.size());
        if (start >= sortedMaterials.size()) return List.of();
        return sortedMaterials.subList(start, end);
    }

    private void render() {
        inventory.clear();
        Set<Material> trashSet = plugin.getTrashRepo().getCachedTrash(player.getUniqueId());
        List<Material> page = currentPageItems();

        for (int i = 0; i < page.size(); i++) {
            Material mat = page.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            boolean isTrashed = trashSet != null && trashSet.contains(mat);
            int count = trashCounts.getOrDefault(mat, 0);

            // decoration(ITALIC, false) is needed because Minecraft adds italic to all
            // custom display names and lore by default.
            meta.displayName(Component.text(MaterialUtil.formatName(mat),
                    isTrashed ? NamedTextColor.RED : NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            if (count > 0) {
                lore.add(Component.text("Trashed by " + count + " player" + (count == 1 ? "" : "s"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (isTrashed) {
                lore.add(Component.text("Currently trashing", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Click to remove", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                // enchant glow as a visual indicator that this item is being trashed.
                // HIDE_ENCHANTS to hide the actual enchants, we override the enchant text
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(Component.text("Click to add to trash", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        renderNavBar();
    }

    private void renderNavBar() {
        int totalPages = totalPages();

        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("Previous Page", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            prev.setItemMeta(prevMeta);
            inventory.setItem(45, prev);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back to Menu", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        inventory.setItem(48, back);

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.displayName(Component.text("Page " + (currentPage + 1) + "/" + totalPages, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        pageInfo.setItemMeta(pageMeta);
        inventory.setItem(49, pageInfo);

        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Next Page", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            next.setItemMeta(nextMeta);
            inventory.setItem(53, next);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        List<Material> page = currentPageItems();

        if (slot < ITEMS_PER_PAGE && slot < page.size()) {
            Material mat = page.get(slot);
            Set<Material> trashSet = plugin.getTrashRepo().getCachedTrash(player.getUniqueId());
            if (trashSet == null) return;

            if (trashSet.contains(mat)) {
                plugin.getTrashRepo().removeMaterial(player.getUniqueId(), mat);
                player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                        .append(Component.text(MaterialUtil.formatName(mat), NamedTextColor.WHITE))
                        .append(Component.text(" from trash list.", NamedTextColor.RED)));
            } else {
                plugin.getTrashRepo().addMaterial(player.getUniqueId(), mat);
                player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                        .append(Component.text(MaterialUtil.formatName(mat), NamedTextColor.WHITE))
                        .append(Component.text(" to trash list.", NamedTextColor.GREEN)));
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            render();
        } else if (slot == 45 && currentPage > 0) {
            currentPage--;
            render();
        } else if (slot == 48) {
            MainMenuGui.open(plugin, player);
        } else if (slot == 53 && currentPage < totalPages() - 1) {
            currentPage++;
            render();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // Opens async: DB query runs off the main thread, then hops back to open the inventory.
    // This prevents lag spikes from the aggregation query on servers with many players.
    public static void open(AutoTrash plugin, Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<Material, Integer> counts = plugin.getTrashRepo().getAllTrashCounts();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    BrowseAllGui gui = new BrowseAllGui(plugin, player, counts);
                    gui.render();
                    player.openInventory(gui.getInventory());
                });
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(Component.text("Failed to load item data.", NamedTextColor.RED)));
            }
        });
    }
}
