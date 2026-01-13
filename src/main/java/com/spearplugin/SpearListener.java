package com.spearplugin;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Iterator;
import java.util.UUID;

public class SpearListener implements Listener {

    private final SpearPlugin plugin;
    private final SpearManager spearManager;

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
        if (killer != null && killer.isOnline() && victimSpear != null) {
            ItemStack killerSpearItem = killer.getInventory().getItemInMainHand();
            SpearManager.SpearTier killerTier = spearManager.getTierFromItem(killerSpearItem);
            SpearManager.SpearTier victimTier = spearManager.getTierFromItem(victimSpear);

            if (killerTier != null && victimTier != null) {
                if (victimTier.getLevel() > killerTier.getLevel()) {
                    // SWAP/STEAL: Killer gets Victim Tier

                    // Remove Victim's High Tier Spear from DROPS
                    event.getDrops().remove(victimSpear);

                    // Give Killer the High Tier Spear
                    ItemStack newKillerSpear = spearManager.getSpear(victimTier);
                    killer.getInventory().setItemInMainHand(newKillerSpear);
                    killer.sendMessage(ChatColor.GREEN + "You STOLE " + victim.getName() + "'s "
                            + victimTier.getDisplayName() + "!");
                    killer.playSound(killer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    // We do NOT give the victim the old spear. They just lose theirs.
                    victim.sendMessage(ChatColor.RED + "Your spear was stolen by " + killer.getName() + "!");

                } else if (killerTier.getLevel() >= victimTier.getLevel()) {
                    // Level Up logic for Killer
                    SpearManager.SpearTier nextTier = spearManager.getNextTier(killerTier);
                    if (nextTier != null) {
                        killer.getInventory().setItemInMainHand(spearManager.getSpear(nextTier));
                        killer.sendMessage(
                                ChatColor.GREEN + "Level Up! You now have a " + nextTier.getDisplayName() + "!");
                        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    } else {
                        killer.sendMessage(ChatColor.GOLD + "Max Level reached!");
                    }
                    // Victim spear remains in drops (dropped naturally)
                }
            }
        }
    }
}
