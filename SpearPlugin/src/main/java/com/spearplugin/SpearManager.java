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
        WOOD(1, "Wood Spear", Material.WOODEN_SWORD, 0, 0),
        STONE(2, "Stone Spear", Material.STONE_SWORD, 1, 0),
        COPPER(3, "Copper Spear", Material.GOLDEN_SWORD, 2, 1), // Using Gold for visual distinction of Copper? Or maybe
                                                                // Stone?
        // Actually, user wants Gold Tier later. Maybe use Stone Sword with CMD if
        // resource pack.
        // I will use GOLDEN_SWORD for Copper (looks coppery) and maybe differentiate
        // Gold Spear with something else or just same material different name/enchants.
        // Actually, let's use IRON_SWORD for Iron, DIAMOND for Diamond.
        // Issue: Copper vs Gold visuals.
        // I'll use GOLDEN_SWORD for Copper and for Gold, relying on name/glint/lore.
        GOLD(4, "Gold Spear", Material.GOLDEN_SWORD, 4, 1),
        IRON(5, "Iron Spear", Material.IRON_SWORD, 5, 2),
        DIAMOND(6, "Diamond Spear", Material.DIAMOND_SWORD, 7, 3),
        NETHERITE(7, "Netherite Spear", Material.NETHERITE_SWORD, 10, 5);

        private final int level;
        private final String displayName;
        private final Material material;
        private final int sharpness;
        private final int lunge;

        SpearTier(int level, String displayName, Material material, int sharpness, int lunge) {
            this.level = level;
            this.displayName = displayName;
            this.material = material;
            this.sharpness = sharpness;
            this.lunge = lunge;
        }

        public int getLevel() {
            return level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getMaterial() {
            return material;
        }

        public int getSharpness() {
            return sharpness;
        }

        public int getLunge() {
            return lunge;
        }
    }

    public ItemStack getSpear(SpearTier tier) {
        ItemStack item = new ItemStack(tier.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6" + tier.getDisplayName()));
            meta.setUnbreakable(true);

            // Attributes
            // Remove vanilla attack speed/damage to normalize? Or keep vanilla?
            // "Spear" usually implies slower/harder? Or just standard sword. I'll keep
            // vanilla for now.

            // Enchants
            if (tier.getSharpness() > 0) {
                meta.addEnchant(Enchantment.SHARPNESS, tier.getSharpness(), true);
            }

            if (tier.getLunge() > 0) {
                Enchantment lunge = Enchantment.getByKey(NamespacedKey.minecraft("lunge"));
                if (lunge != null) {
                    meta.addEnchant(lunge, tier.getLunge(), true);
                }
            }

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + tier.getLevel());
            lore.add(ChatColor.GOLD + "Unbreakable");
            meta.setLore(lore);

            // PDC
            meta.getPersistentDataContainer().set(SPEAR_LEVEL_KEY, PersistentDataType.INTEGER, tier.level);

            // Flags
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Optional, maybe cleaner.

            item.setItemMeta(meta);
        }
        return item;
    }

    public SpearTier getNextTier(SpearTier current) {
        // Find next level
        for (SpearTier tier : SpearTier.values()) {
            if (tier.level == current.level + 1) {
                return tier;
            }
        }
        return null; // Max level
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
