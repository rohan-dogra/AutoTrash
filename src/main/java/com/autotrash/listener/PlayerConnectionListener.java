package com.autotrash.listener;

import com.autotrash.AutoTrash;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final AutoTrash plugin;

    public PlayerConnectionListener(AutoTrash plugin) {
        this.plugin = plugin;
    }

    // lowest priority so the cache load starts as early as possible after join.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getTrashRepo().loadPlayerAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTrashRepo().uncachePlayer(event.getPlayer().getUniqueId());
    }
}
