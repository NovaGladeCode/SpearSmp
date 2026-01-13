package com.spearplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SpearPlugin extends JavaPlugin implements CommandExecutor {

    private SpearManager spearManager;

    @Override
    public void onEnable() {
        spearManager = new SpearManager(this);
        getServer().getPluginManager().registerEvents(new SpearListener(this, spearManager), this);

        getCommand("getspear").setExecutor(this);

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
            } else {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
        }
        return false;
    }
}
