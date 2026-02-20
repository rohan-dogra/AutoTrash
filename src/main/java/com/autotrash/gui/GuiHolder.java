package com.autotrash.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

// Sealed so only our GUI classes can implement it. GuiListener uses instanceof
// to identify GUI inventories and delegate clicks to the correct handler.
public sealed interface GuiHolder extends InventoryHolder
        permits MainMenuGui, BrowseAllGui, MostTrashedGui, MyTrashListGui {

    void handleClick(InventoryClickEvent event);
}
