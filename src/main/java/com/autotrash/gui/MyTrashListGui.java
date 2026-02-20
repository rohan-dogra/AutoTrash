package com.autotrash.gui;

import com.autotrash.AutoTrash;
import com.autotrash.util.MaterialUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class MyTrashListGui implements GuiHolder {

    private static final int ITEMS_PER_PAGE = 45;

    private final AutoTrash plugin;
    private final Player player;
    private final Inventory inventory;
    private int currentPage = 0;

    private MyTrashListGui(AutoTrash plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text("My Trash List", NamedTextColor.DARK_RED, TextDecoration.BOLD));
    }

    private List<Material> getSortedTrashList() {
        Set<Material> trashSet = plugin.getTrashRepo().getCachedTrash(player.getUniqueId());
        if (trashSet == null || trashSet.isEmpty()) return List.of();
        return trashSet.stream()
                .sorted(Comparator.comparing(Material::name))
                .toList();
    }

    private void render() {
        inventory.clear();
        List<Material> sorted = getSortedTrashList();

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, sorted.size());

        for (int i = start; i < end; i++) {
            Material mat = sorted.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(Component.text(MaterialUtil.formatName(mat), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Click to remove", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));

            item.setItemMeta(meta);
            inventory.setItem(i - start, item);
        }

        renderNavBar(sorted.size());
    }

    private void renderNavBar(int totalItems) {
        int totalPages = Math.max(1, (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);

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
        List<Material> sorted = getSortedTrashList();
        int index = currentPage * ITEMS_PER_PAGE + slot;

        if (slot < ITEMS_PER_PAGE && index < sorted.size()) {
            Material mat = sorted.get(index);
            plugin.getTrashRepo().removeMaterial(player.getUniqueId(), mat);
            player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                    .append(Component.text(MaterialUtil.formatName(mat), NamedTextColor.WHITE))
                    .append(Component.text(" from trash list.", NamedTextColor.RED)));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

            // Adjust page if we removed the last item on this page
            List<Material> updated = getSortedTrashList();
            int newTotalPages = Math.max(1, (updated.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
            if (currentPage >= newTotalPages) {
                currentPage = Math.max(0, newTotalPages - 1);
            }
            render();
        } else if (slot == 45 && currentPage > 0) {
            currentPage--;
            render();
        } else if (slot == 48) {
            MainMenuGui.open(plugin, player);
        } else if (slot == 53) {
            List<Material> current = getSortedTrashList();
            int totalPages = Math.max(1, (current.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                render();
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(AutoTrash plugin, Player player) {
        MyTrashListGui gui = new MyTrashListGui(plugin, player);
        gui.render();
        player.openInventory(gui.getInventory());
    }
}
