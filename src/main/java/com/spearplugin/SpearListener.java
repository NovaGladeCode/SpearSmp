package com.spearplugin;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SpearListener implements Listener {

    private final SpearPlugin plugin;
    private final SpearManager spearManager;
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
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.getInventory().addItem(spear);
            }, 1L);
        } else {
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
        Player killer = victim.getKiller();

        ItemStack victimSpear = null;

        // 1. Find and Save Victim's current spear
        Iterator<ItemStack> iter = event.getDrops().iterator();
        while (iter.hasNext()) {
            ItemStack drop = iter.next();
            if (spearManager.getTierFromItem(drop) != null) {
                victimSpear = drop;
                iter.remove();
                break;
            }
        }
        if (victimSpear == null) {
            // Check main hand or other slots if not in drops (KeepInv edge case)
            ItemStack hand = victim.getInventory().getItemInMainHand();
            if (spearManager.getTierFromItem(hand) != null) {
                victimSpear = hand;
            }
        }

        // 2. Logic for Swapping/Leveling
        if (killer != null && killer.isOnline() && victimSpear != null) {
            ItemStack killerSpearItem = killer.getInventory().getItemInMainHand();
            SpearManager.SpearTier killerTier = spearManager.getTierFromItem(killerSpearItem);
            SpearManager.SpearTier victimTier = spearManager.getTierFromItem(victimSpear);

            if (killerTier != null && victimTier != null) {
                if (victimTier.getLevel() > killerTier.getLevel()) {
                    // SWAP: Killer gets Victim Tier, Victim gets Killer Tier

                    // Update Killer
                    ItemStack newKillerSpear = spearManager.getSpear(victimTier);
                    killer.getInventory().setItemInMainHand(newKillerSpear);
                    killer.sendMessage(ChatColor.GREEN + "You STOLE " + victim.getName() + "'s "
                            + victimTier.getDisplayName() + "!");
                    killer.playSound(killer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    // Update Victim (Saved Spear)
                    ItemStack newVictimSpear = spearManager.getSpear(killerTier);
                    savedSpears.put(victim.getUniqueId(), newVictimSpear);
                    victim.sendMessage(ChatColor.RED + "You were demoted by " + killer.getName() + "! You now have a "
                            + killerTier.getDisplayName() + ".");

                } else if (killerTier.getLevel() >= victimTier.getLevel()) {
                    // Standard Level Up for Killer
                    SpearManager.SpearTier nextTier = spearManager.getNextTier(killerTier);
                    if (nextTier != null) {
                        killer.getInventory().setItemInMainHand(spearManager.getSpear(nextTier));
                        killer.sendMessage(
                                ChatColor.GREEN + "Level Up! You now have a " + nextTier.getDisplayName() + "!");
                        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    } else {
                        killer.sendMessage(ChatColor.GOLD + "Max Level reached!");
                    }
                    // Victim keeps their spear (no change) regarding level, just saved.
                    savedSpears.put(victim.getUniqueId(), victimSpear);
                }
            } else {
                // If killer has no spear (unlikely if they killed with it, but maybe bow?)
                // Just save victim spear normally.
                savedSpears.put(victim.getUniqueId(), victimSpear);
            }
        } else {
            // PvE Death or no spear involved
            if (victimSpear != null) {
                savedSpears.put(victim.getUniqueId(), victimSpear);
            }
        }
    }
}
