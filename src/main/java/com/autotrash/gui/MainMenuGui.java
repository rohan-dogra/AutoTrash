package com.autotrash.gui;

import com.autotrash.AutoTrash;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class MainMenuGui implements GuiHolder {

    private final AutoTrash plugin;
    private final Player player;
    private final Inventory inventory;

    private MainMenuGui(AutoTrash plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27,
                Component.text("AutoTrash", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        render();
    }

    private void render() {
        ItemStack browse = new ItemStack(Material.COMPASS);
        ItemMeta browseMeta = browse.getItemMeta();
        browseMeta.displayName(Component.text("Browse All Items", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        browseMeta.lore(List.of(Component.text("Click to browse all items", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        browse.setItemMeta(browseMeta);
        inventory.setItem(11, browse);

        ItemStack popular = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta popularMeta = popular.getItemMeta();
        popularMeta.displayName(Component.text("Most Trashed", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        popularMeta.lore(List.of(Component.text("See what others are trashing", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        popular.setItemMeta(popularMeta);
        inventory.setItem(13, popular);

        ItemStack myList = new ItemStack(Material.CHEST);
        ItemMeta myListMeta = myList.getItemMeta();
        myListMeta.displayName(Component.text("My Trash List", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        myListMeta.lore(List.of(Component.text("View your trashed items", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        myList.setItemMeta(myListMeta);
        inventory.setItem(15, myList);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case 11 -> BrowseAllGui.open(plugin, player);
            case 13 -> MostTrashedGui.open(plugin, player);
            case 15 -> MyTrashListGui.open(plugin, player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(AutoTrash plugin, Player player) {
        player.openInventory(new MainMenuGui(plugin, player).getInventory());
    }
}
