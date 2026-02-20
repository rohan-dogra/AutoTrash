package com.autotrash.util;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class MaterialUtil {

    // pre-computed once at class load. filters out non-item materials (like air)
    // and LEGACY_ materials from pre-1.13 that Bukkit keeps for backwards compat.
    private static final List<Material> PICKUP_MATERIALS;
    private static final int ITEMS_PER_PAGE = 45;

    static {
        PICKUP_MATERIALS = Arrays.stream(Material.values())
                .filter(Material::isItem)
                .filter(m -> !m.isAir())
                .filter(m -> !m.name().startsWith("LEGACY_"))
                .sorted(Comparator.comparing(Material::name))
                .collect(Collectors.toUnmodifiableList());
    }

    private MaterialUtil() {}

    public static List<Material> getPickupMaterials() {
        return PICKUP_MATERIALS;
    }

    public static int pageCount() {
        return (PICKUP_MATERIALS.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
    }

    public static List<Material> getPage(int page) {
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, PICKUP_MATERIALS.size());
        if (start >= PICKUP_MATERIALS.size()) return List.of();
        return PICKUP_MATERIALS.subList(start, end);
    }

    // Converts rotten_flesh -> "Rotten Flesh" for display in GUIs and chat.
    public static String formatName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(parts[i].charAt(0));
            sb.append(parts[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
