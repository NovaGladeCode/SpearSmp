package com.spearplugin;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class SpearListener implements Listener {
    private final SpearPlugin plugin;
    private final SpearManager spearManager;

    public SpearListener(SpearPlugin plugin, SpearManager spearManager) {
        this.plugin = plugin;
        this.spearManager = spearManager;
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && killer.isOnline()) {
            ItemStack mainHand = killer.getInventory().getItemInMainHand();
            SpearManager.SpearTier currentTier = spearManager.getTierFromItem(mainHand);

            if (currentTier != null) {
                SpearManager.SpearTier nextTier = spearManager.getNextTier(currentTier);
                if (nextTier != null) {
                    // Update item
                    ItemStack nextSpear = spearManager.getSpear(nextTier);
                    // Preserve durability? It's unbreakable.
                    // Preserve other things? Nah, just replace.
                    killer.getInventory().setItemInMainHand(nextSpear);
                    killer.sendMessage(ChatColor.GREEN + "Your Spear leveled up to " + nextTier.getDisplayName() + "!");
                    killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                } else {
                    killer.sendMessage(ChatColor.GOLD + "Your Spear is already at Max Level!");
                }
            }
        }
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (spearManager.getTierFromItem(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your spear!");
        }
    }
}
