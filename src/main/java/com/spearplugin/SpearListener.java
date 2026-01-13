package com.spearplugin;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SpearListener implements Listener {

    private final SpearPlugin plugin;
    private final SpearManager spearManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ItemStack> savedSpears = new HashMap<>();

    public SpearListener(SpearPlugin plugin, SpearManager spearManager) {
        this.plugin = plugin;
        this.spearManager = spearManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ensureHasSpear(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (savedSpears.containsKey(player.getUniqueId())) {
            ItemStack spear = savedSpears.remove(player.getUniqueId());
            // Give it back a tick later to ensure inventory exists/is ready
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.getInventory().addItem(spear);
            }, 1L);
        } else {
            // If they somehow didn't have one saved, ensure they get a starter one
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> ensureHasSpear(player), 2L);
        }
    }

    private void ensureHasSpear(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (spearManager.getTierFromItem(item) != null) {
                return;
            }
        }
        player.getInventory().addItem(spearManager.getSpear(SpearManager.SpearTier.WOOD));
        player.sendMessage(ChatColor.YELLOW + "You received your starting Spear!");
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (spearManager.getTierFromItem(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your Spear!");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // Save victim's spear and remove from drops
        Iterator<ItemStack> iter = event.getDrops().iterator();
        ItemStack spearToSave = null;
        while (iter.hasNext()) {
            ItemStack drop = iter.next();
            if (spearManager.getTierFromItem(drop) != null) {
                spearToSave = drop;
                iter.remove(); // Don't drop it on the ground
                break; // Assume only one spear
            }
        }

        // Also check inventory in case it wasn't dropped (e.g. keepInventory rules
        // conflict,
        // though getDrops usually has them).
        // If we found one in drops, save it.
        if (spearToSave != null) {
            savedSpears.put(victim.getUniqueId(), spearToSave);
        } else {
            // Check main hand just in case logic varies
            ItemStack hand = victim.getInventory().getItemInMainHand();
            if (spearManager.getTierFromItem(hand) != null) {
                savedSpears.put(victim.getUniqueId(), hand);
                // It might not be in drops if keepInventory is on, but we want to be sure.
            }
        }

        // Handle Killer Level Up
        Player killer = victim.getKiller();
        if (killer != null && killer.isOnline()) {
            ItemStack mainHand = killer.getInventory().getItemInMainHand();
            SpearManager.SpearTier currentTier = spearManager.getTierFromItem(mainHand);

            if (currentTier != null) {
                SpearManager.SpearTier nextTier = spearManager.getNextTier(currentTier);
                if (nextTier != null) {
                    ItemStack nextSpear = spearManager.getSpear(nextTier);
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
                long now = System.currentTimeMillis();
                long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
                long cooldownTime = 2000;

                if (now - lastUsed < cooldownTime) {
                    player.sendMessage(ChatColor.RED + "Lunge is on cooldown!");
                    return;
                }

                double strength = 1.0 + (tier.getLunge() * 0.3);
                Vector direction = player.getLocation().getDirection().multiply(strength);
                if (player.isOnGround()) {
                    direction.setY(0.5);
                }

                player.setVelocity(direction);
                player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1f);

                cooldowns.put(player.getUniqueId(), now);
                event.setCancelled(true);
            }
        }
    }
}
