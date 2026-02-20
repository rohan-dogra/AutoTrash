package com.autotrash.command;

import com.autotrash.AutoTrash;
import com.autotrash.gui.MainMenuGui;
import com.autotrash.util.MaterialUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TrashCommand {

    private final AutoTrash plugin;

    public TrashCommand(AutoTrash plugin) {
        this.plugin = plugin;
    }

    public void register(Commands registrar) {
        registrar.register(
            Commands.literal("autotrash")
                .requires(src -> src.getSender().hasPermission("autotrash.use"))
                .executes(this::handleOpenMenu)
                .then(Commands.literal("add")
                    .then(Commands.argument("material", StringArgumentType.word())
                        .suggests(this::suggestAllMaterials)
                        .executes(this::handleAdd)))
                .then(Commands.literal("remove")
                    .then(Commands.argument("material", StringArgumentType.word())
                        .suggests(this::suggestPlayerMaterials)
                        .executes(this::handleRemove)))
                .then(Commands.literal("list")
                    .executes(this::handleList))
                .then(Commands.literal("reload")
                    .requires(src -> src.getSender().hasPermission("autotrash.admin"))
                    .executes(this::handleReload))
                .build(),
            "Manage auto-trashed items",
            List.of("atrash")
        );
    }

    private int handleOpenMenu(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Only players can use this command.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        MainMenuGui.open(plugin, player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleAdd(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Only players can use this command.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String name = StringArgumentType.getString(ctx, "material");
        Material material;
        try {
            material = Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown material: " + name, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!material.isItem()) {
            player.sendMessage(Component.text("That material is not a pickup-able item.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (plugin.getTrashRepo().addMaterial(player.getUniqueId(), material)) {
            player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                    .append(Component.text(MaterialUtil.formatName(material), NamedTextColor.WHITE))
                    .append(Component.text(" to trash list.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("That item is already in your trash list.", NamedTextColor.YELLOW));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleRemove(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Only players can use this command.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String name = StringArgumentType.getString(ctx, "material");
        Material material;
        try {
            material = Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown material: " + name, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (plugin.getTrashRepo().removeMaterial(player.getUniqueId(), material)) {
            player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                    .append(Component.text(MaterialUtil.formatName(material), NamedTextColor.WHITE))
                    .append(Component.text(" from trash list.", NamedTextColor.RED)));
        } else {
            player.sendMessage(Component.text("That item is not in your trash list.", NamedTextColor.YELLOW));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleList(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Only players can use this command.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Set<Material> items = plugin.getTrashRepo().getCachedTrash(player.getUniqueId());
        if (items == null || items.isEmpty()) {
            player.sendMessage(Component.text("Your trash list is empty.", NamedTextColor.YELLOW));
        } else {
            String list = items.stream()
                    .sorted(Comparator.comparing(Material::name))
                    .map(MaterialUtil::formatName)
                    .collect(Collectors.joining(", "));
            player.sendMessage(Component.text("Trashed items: ", NamedTextColor.GOLD)
                    .append(Component.text(list, NamedTextColor.WHITE)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleReload(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadPluginConfig();
        ctx.getSource().getSender().sendMessage(
                Component.text("AutoTrash config reloaded.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestAllMaterials(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        MaterialUtil.getPickupMaterials().stream()
                .map(m -> m.name().toLowerCase())
                .filter(name -> name.startsWith(input))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    //only suggests materials already in the player's trash list (reads from cache, no DB hit).
    private CompletableFuture<Suggestions> suggestPlayerMaterials(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (!(ctx.getSource().getSender() instanceof Player player)) return builder.buildFuture();
        String input = builder.getRemaining().toLowerCase();
        Set<Material> trashSet = plugin.getTrashRepo().getCachedTrash(player.getUniqueId());
        if (trashSet != null) {
            trashSet.stream()
                    .map(m -> m.name().toLowerCase())
                    .filter(name -> name.startsWith(input))
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    }
}
