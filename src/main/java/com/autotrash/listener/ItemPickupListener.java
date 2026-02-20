package com.autotrash.listener;

import com.autotrash.AutoTrash;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.Set;

public class ItemPickupListener implements Listener {

    private final AutoTrash plugin;

    public ItemPickupListener(AutoTrash plugin) {
        this.plugin = plugin;
    }

    // highest priority so we run after other plugins that might modify the pickup.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        //if cache isn't loaded yet (brief window after join), allow the pickup.
        Set<Material> trashSet = plugin.getTrashRepo().getCachedTrash(player.getUniqueId());
        if (trashSet != null && trashSet.contains(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
            event.getItem().remove(); // destroy the item entity so it doesn't sit on the ground
        }
    }
}
