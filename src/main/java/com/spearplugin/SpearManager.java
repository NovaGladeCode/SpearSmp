package com.spearplugin;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpearManager {

    private final SpearPlugin plugin;
    public static final NamespacedKey SPEAR_LEVEL_KEY = new NamespacedKey("spearleveling", "level");

    public SpearManager(SpearPlugin plugin) {
        this.plugin = plugin;
    }

    public enum SpearTier {
        WOOD(1, "Wood Spear", "WOODEN_SPEAR", 0, 0),
        STONE(2, "Stone Spear", "STONE_SPEAR", 1, 0),
        COPPER(3, "Copper Spear", "COPPER_SPEAR", 2, 1),
        GOLD(4, "Gold Spear", "GOLDEN_SPEAR", 4, 1),
        IRON(5, "Iron Spear", "IRON_SPEAR", 5, 2),
        DIAMOND(6, "Diamond Spear", "DIAMOND_SPEAR", 7, 3),
        NETHERITE(7, "Netherite Spear", "NETHERITE_SPEAR", 10, 5);

        private final int level;
        private final String displayName;
        private final String materialName;
        private final int sharpness;
        private final int lunge;
        private Material cachedMaterial;

        SpearTier(int level, String displayName, String materialName, int sharpness, int lunge) {
            this.level = level;
            this.displayName = displayName;
            this.materialName = materialName;
            this.sharpness = sharpness;
            this.lunge = lunge;
        }

        public int getLevel() {
            return level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSharpness() {
            return sharpness;
        }

        public int getLunge() {
            return lunge;
        }

        public Material getMaterial() {
            if (cachedMaterial == null) {
                cachedMaterial = Material.getMaterial(materialName);
                if (cachedMaterial == null) {
                    // Fallback to Trident if detailed spears aren't found (compatibility mode)
                    // Or Stone Sword. But try TRIDENT first as it looks like a spear.
                    cachedMaterial = Material.TRIDENT; // Assuming user might be on version w/o spear
                    if (cachedMaterial == null)
                        cachedMaterial = Material.STONE_SWORD;
                }
            }
            return cachedMaterial;
        }
    }

    public ItemStack getSpear(SpearTier tier) {
        ItemStack item = new ItemStack(tier.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6" + tier.getDisplayName()));
            meta.setUnbreakable(true);

            // Enchants
            if (tier.getSharpness() > 0) {
                meta.addEnchant(Enchantment.SHARPNESS, tier.getSharpness(), true);
            }

            // Try Lunge (Native)
            if (tier.getLunge() > 0) {
                Enchantment lunge = Enchantment.getByKey(NamespacedKey.minecraft("lunge"));
                if (lunge != null) {
                    meta.addEnchant(lunge, tier.getLunge(), true);
                }
            }

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + tier.getLevel());
            if (tier.getLunge() > 0) {
                lore.add(ChatColor.GRAY + "Ability: " + ChatColor.AQUA + "Lunge " + tier.getLunge());
            }
            lore.add(ChatColor.GOLD + "Unbreakable");
            lore.add(ChatColor.YELLOW + "Kill higher level players to steal their rank!");
            meta.setLore(lore);

            // PDC
            meta.getPersistentDataContainer().set(SPEAR_LEVEL_KEY, PersistentDataType.INTEGER, tier.level);

            // Flags
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        }
        return item;
    }

    public SpearTier getNextTier(SpearTier current) {
        for (SpearTier tier : SpearTier.values()) {
            if (tier.level == current.level + 1) {
                return tier;
            }
        }
        return null;
    }

    public SpearTier getTier(String name) {
        try {
            return SpearTier.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    public SpearTier getTierFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(SPEAR_LEVEL_KEY,
                PersistentDataType.INTEGER);
        if (level != null) {
            for (SpearTier tier : SpearTier.values()) {
                if (tier.level == level)
                    return tier;
            }
        }
        return null;
    }
}
