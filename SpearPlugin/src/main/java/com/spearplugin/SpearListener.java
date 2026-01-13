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
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpearListener implements Listener {

    private final SpearPlugin plugin;
    private final SpearManager spearManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

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
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            SpearManager.SpearTier tier = spearManager.getTierFromItem(item);

            if (tier != null && tier.getLunge() > 0) {
                // Check Cooldown
                long now = System.currentTimeMillis();
                long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
                long cooldownTime = 2000; // 2 seconds cooldown?

                if (now - lastUsed < cooldownTime) {
                    player.sendMessage(ChatColor.RED + "Lunge is on cooldown!");
                    return;
                }

                // Perform Lunge
                double strength = 1.0 + (tier.getLunge() * 0.3); // Base 1 + 0.3 per level
                Vector direction = player.getLocation().getDirection().multiply(strength);
                // Maybe add a bit of Y lift if on ground to help "lunge"
                if (player.isOnGround()) {
                    direction.setY(0.5);
                }

                player.setVelocity(direction);
                player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1f);

                cooldowns.put(player.getUniqueId(), now);
                event.setCancelled(true); // Prevent incidental block interaction
            }
        }
    }
}
