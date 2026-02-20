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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class MostTrashedGui implements GuiHolder {

    private static final int ITEMS_PER_PAGE = 45;

    private final AutoTrash plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Map.Entry<Material, Integer>> items;
    private int currentPage = 0;

    private MostTrashedGui(AutoTrash plugin, Player player, List<Map.Entry<Material, Integer>> items) {
        this.plugin = plugin;
        this.player = player;
        this.items = items;
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text("Most Trashed", NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    private void render() {
        inventory.clear();
        Set<Material> trashSet = plugin.getTrashRepo().getCachedTrash(player.getUniqueId());

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());

        for (int i = start; i < end; i++) {
            Map.Entry<Material, Integer> entry = items.get(i);
            Material mat = entry.getKey();
            int count = entry.getValue();

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            boolean isTrashed = trashSet != null && trashSet.contains(mat);

            meta.displayName(Component.text(MaterialUtil.formatName(mat),
                    isTrashed ? NamedTextColor.RED : NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Trashed by " + count + " player" + (count == 1 ? "" : "s"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));

            if (isTrashed) {
                lore.add(Component.text("Click to remove", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(Component.text("Click to add to trash", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i - start, item);
        }

        renderNavBar();
    }

    private void renderNavBar() {
        int totalPages = Math.max(1, (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);

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
        int index = currentPage * ITEMS_PER_PAGE + slot;

        if (slot < ITEMS_PER_PAGE && index < items.size()) {
            Material mat = items.get(index).getKey();
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
        } else if (slot == 53 && currentPage < Math.max(1, (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE) - 1) {
            currentPage++;
            render();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    //opens async. DB agg query runs off the main thread.
    public static void open(AutoTrash plugin, Player player) {
        int limit = plugin.getPluginConfig().getMostTrashedLimit();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map.Entry<Material, Integer>> mostTrashed = plugin.getTrashRepo().getMostTrashed(limit);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return; // player may have left during async query
                    MostTrashedGui gui = new MostTrashedGui(plugin, player, mostTrashed);
                    gui.render();
                    player.openInventory(gui.getInventory());
                });
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(Component.text("Failed to load most trashed data.", NamedTextColor.RED)));
            }
        });
    }
}
