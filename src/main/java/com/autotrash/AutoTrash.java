package com.autotrash;

import com.autotrash.command.TrashCommand;
import com.autotrash.config.PluginConfig;
import com.autotrash.gui.GuiListener;
import com.autotrash.listener.ItemPickupListener;
import com.autotrash.listener.PlayerConnectionListener;
import com.autotrash.storage.DatabaseManager;
import com.autotrash.storage.TrashRepository;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class AutoTrash extends JavaPlugin {

    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private TrashRepository trashRepo;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(getConfig());

        this.databaseManager = new DatabaseManager(getDataFolder().toPath(), getLogger());
        try {
            databaseManager.initialize();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.trashRepo = new TrashRepository(databaseManager, this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            new TrashCommand(this).register(event.registrar());
        });

        getServer().getPluginManager().registerEvents(new ItemPickupListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        getLogger().info("AutoTrash enabled.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("AutoTrash disabled.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.pluginConfig = new PluginConfig(getConfig());
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public TrashRepository getTrashRepo() {
        return trashRepo;
    }
}
