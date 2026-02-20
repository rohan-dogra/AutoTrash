package com.autotrash.storage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Handles all trash list CRUD operations with an in-memory cache layer.
 * Cache is the source of truth for reads (pickup listener), with async DB writes for persistence.
 * This avoids hitting SQLite on every item pickup event.
 */
public final class TrashRepository {
    private final DatabaseManager db;
    private final Logger logger;
    private final Plugin plugin;

    // Uses ConcurrentHashMap.newKeySet() for values instead of EnumSet because
    // the pickup listener and async DB callbacks access the same sets from different threads.
    private final ConcurrentHashMap<UUID, Set<Material>> cache = new ConcurrentHashMap<>();

    public TrashRepository(DatabaseManager db, Plugin plugin) {
        this.db = db;
        this.logger = plugin.getLogger();
        this.plugin = plugin;
    }

    /**
     * loads a player's trash list from db. called async on join.
     * unknown materials (removed in a Minecraft update) are logged and skipped.
     */
    public Set<Material> loadFromDatabase(UUID playerId) throws SQLException {
        Set<Material> set = ConcurrentHashMap.newKeySet();
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT material FROM player_trash WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    set.add(Material.valueOf(rs.getString("material")));
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown material in DB: " + rs.getString("material"));
                }
            }
        }
        return set;
    }

    public void cachePlayer(UUID playerId, Set<Material> materials) {
        cache.put(playerId, materials);
    }

    public void uncachePlayer(UUID playerId) {
        cache.remove(playerId);
    }

    public Set<Material> getCachedTrash(UUID playerId) {
        return cache.get(playerId);
    }

    // updates cache immediately for instant effect, then persists to DB async.
    // returns false if the item was already in the list or cache isn't loaded yet.
    public boolean addMaterial(UUID playerId, Material material) {
        Set<Material> set = cache.get(playerId);
        if (set == null) return false;
        boolean added = set.add(material);
        if (added) {
            CompletableFuture.runAsync(() -> {
                try (PreparedStatement ps = db.getConnection().prepareStatement(
                        "INSERT INTO player_trash (player_uuid, material) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                    ps.setString(1, playerId.toString());
                    ps.setString(2, material.name());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.warning("Failed to persist trash add for " + playerId + ": " + e.getMessage());
                }
            });
        }
        return added;
    }

    public boolean removeMaterial(UUID playerId, Material material) {
        Set<Material> set = cache.get(playerId);
        if (set == null) return false;
        boolean removed = set.remove(material);
        if (removed) {
            CompletableFuture.runAsync(() -> {
                try (PreparedStatement ps = db.getConnection().prepareStatement(
                        "DELETE FROM player_trash WHERE player_uuid = ? AND material = ?")) {
                    ps.setString(1, playerId.toString());
                    ps.setString(2, material.name());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.warning("Failed to persist trash remove for " + playerId + ": " + e.getMessage());
                }
            });
        }
        return removed;
    }

    public void loadPlayerAsync(UUID playerId) {
        CompletableFuture.runAsync(() -> {
            try {
                Set<Material> materials = loadFromDatabase(playerId);
                Bukkit.getScheduler().runTask(plugin, () -> cachePlayer(playerId, materials));
            } catch (SQLException e) {
                logger.warning("Failed to load trash for " + playerId + ": " + e.getMessage());
            }
        });
    }

    public Map<Material, Integer> getAllTrashCounts() throws SQLException {
        Map<Material, Integer> counts = new HashMap<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT material, COUNT(*) AS cnt FROM player_trash GROUP BY material")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    counts.put(Material.valueOf(rs.getString("material")), rs.getInt("cnt"));
                } catch (IllegalArgumentException e) {
                    // skip unknown materials
                }
            }
        }
        return counts;
    }

    public List<Map.Entry<Material, Integer>> getMostTrashed(int limit) throws SQLException {
        List<Map.Entry<Material, Integer>> result = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT material, COUNT(*) AS cnt FROM player_trash GROUP BY material ORDER BY cnt DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Material mat = Material.valueOf(rs.getString("material"));
                    result.add(Map.entry(mat, rs.getInt("cnt")));
                } catch (IllegalArgumentException e) {
                    // skip unknown materials
                }
            }
        }
        return result;
    }
}
