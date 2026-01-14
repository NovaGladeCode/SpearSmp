package com.spearplugin;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import java.util.Iterator;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class SpearListener implements Listener {

    private final SpearPlugin plugin;
    private final SpearManager spearManager;
    private final Map<UUID, Long> lungeCooldowns = new HashMap<>();

    public SpearListener(SpearPlugin plugin, SpearManager spearManager) {
        this.plugin = plugin;
        this.spearManager = spearManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        ItemStack victimSpear = null;

        // 1. Find Victim's spear in drops
        Iterator<ItemStack> iter = event.getDrops().iterator();
        while (iter.hasNext()) {
            ItemStack drop = iter.next();
            if (spearManager.getTierFromItem(drop) != null) {
                victimSpear = drop;
                break;
            }
        }

        // 2. Logic for Swapping/Leveling
        // We only care if the KILLER has a spear. If so, they deserve a level up (or
        // swap).
        if (killer != null && killer.isOnline()) {
            ItemStack killerSpearItem = killer.getInventory().getItemInMainHand();
            SpearManager.SpearTier killerTier = spearManager.getTierFromItem(killerSpearItem);

            // Only proceed if killer actually used a spear (or is holding one, ensuring
            // they are participating)
            if (killerTier != null) {
                SpearManager.SpearTier victimTier = (victimSpear != null) ? spearManager.getTierFromItem(victimSpear)
                        : null;

                // Check for Steal Condition: Victim must have a spear AND it must be better
                if (victimTier != null && victimTier.getLevel() > killerTier.getLevel()) {
                    // SWAP/STEAL + UPGRADE: Killer gets Victim Tier + 1

                    event.getDrops().remove(victimSpear);

                    // Determine new tier: Victim's Tier + 1
                    SpearManager.SpearTier targetTier = spearManager.getNextTier(victimTier);
                    if (targetTier == null)
                        targetTier = victimTier; // Max level

                    ItemStack newKillerSpear = spearManager.getSpear(targetTier);
                    killer.getInventory().setItemInMainHand(newKillerSpear);
                    killer.sendMessage(ChatColor.GREEN + "You STOLE " + victim.getName() + "'s rank and Leveled Up to "
                            + targetTier.getDisplayName() + "!");
                    killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                    victim.sendMessage(ChatColor.RED + "Your spear was stolen by " + killer.getName() + "!");

                } else {
                    // Standard Level Up logic (Killer Tier + 1)
                    // Happens if victim has NO spear OR victim has WORSE/EQUAL spear

                    SpearManager.SpearTier nextTier = spearManager.getNextTier(killerTier);
                    if (nextTier != null) {
                        killer.getInventory().setItemInMainHand(spearManager.getSpear(nextTier));
                        killer.sendMessage(
                                ChatColor.GREEN + "Level Up! You now have a " + nextTier.getDisplayName() + "!");
                        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    } else {
                        killer.sendMessage(ChatColor.GOLD + "Max Level reached!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (spearManager.getTierFromItem(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your spear!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        SpearManager.SpearTier tier = spearManager.getTierFromItem(item);

        if (tier == null)
            return;

        // Prevent throwing the spear (Trident behavior) or blocking weirdly
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Only handle main hand to avoid double firing
            if (event.getHand() != EquipmentSlot.HAND)
                return;

            event.setCancelled(true); // Cancel default trident throw/block interaction

            int lungeLevel = tier.getLunge();
            if (lungeLevel > 0) {
                // Check Cooldown
                long now = System.currentTimeMillis();
                if (lungeCooldowns.containsKey(player.getUniqueId())) {
                    long lastUse = lungeCooldowns.get(player.getUniqueId());
                    if (now - lastUse < 2000) { // 2 seconds cooldown
                        return; // On cooldown
                    }
                }

                // Perform Lunge
                lungeCooldowns.put(player.getUniqueId(), now);

                // Vector math: Forward + slightly up
                org.bukkit.util.Vector direction = player.getLocation().getDirection();
                // Normalize and scale. Power increases with lunge level.
                double power = 1.2 + (lungeLevel * 0.25);
                player.setVelocity(direction.multiply(power).setY(0.4)); // Fixed Y boost for hop

                player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
                player.sendMessage(ChatColor.AQUA + "Lunge!");
            }
        }
    }
}
