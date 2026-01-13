package com.spearplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpearPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    private SpearManager spearManager;

    @Override
    public void onEnable() {
        spearManager = new SpearManager(this);
        getServer().getPluginManager().registerEvents(new SpearListener(this, spearManager), this);

        getCommand("getspear").setExecutor(this); // Alias/Old
        getCommand("spear").setExecutor(this);
        getCommand("spear").setTabCompleter(this);

        getLogger().info("SpearLeveling has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SpearLeveling has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("getspear")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.getInventory().addItem(spearManager.getSpear(SpearManager.SpearTier.WOOD));
                player.sendMessage(ChatColor.GREEN + "You received a Wood Spear!");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("spear")) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("set")) {
                if (!sender.hasPermission("spear.set")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }

                String tierName = args[1];
                SpearManager.SpearTier tier = spearManager.getTier(tierName);
                if (tier == null) {
                    sender.sendMessage(ChatColor.RED
                            + "Invalid Spear Tier! Use: Wood, Stone, Copper, Gold, Iron, Diamond, Netherite");
                    return true;
                }

                Player target = null;
                if (args.length >= 3) {
                    target = getServer().getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                } else if (sender instanceof Player) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player.");
                    return true;
                }

                spearManager.setSpear(target, tier);
                sender.sendMessage(ChatColor.GREEN + "Gave " + tier.getDisplayName() + " to " + target.getName());
                target.sendMessage(ChatColor.GREEN + "You received a " + tier.getDisplayName() + "!");
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("spear")) {
            if (args.length == 1) {
                List<String> sub = new ArrayList<>();
                sub.add("set");
                return sub;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                List<String> tiers = new ArrayList<>();
                for (SpearManager.SpearTier t : SpearManager.SpearTier.values()) {
                    tiers.add(t.name());
                }
                return tiers;
            }
        }
        return null;
    }
}
