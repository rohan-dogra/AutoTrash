package com.autotrash.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;
        event.setCancelled(true); // always cancel first to so users dont yoink the gui items to their inv lol

        //ignore clicks in the player's own inventory (bottom half of the screen)
        if (event.getClickedInventory() != event.getInventory()) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        holder.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }
}
