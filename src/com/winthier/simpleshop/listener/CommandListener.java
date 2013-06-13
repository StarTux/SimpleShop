package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.sql.LogImporter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListener implements CommandExecutor {
        private SimpleShopPlugin plugin;

        public CommandListener(SimpleShopPlugin plugin) {
                this.plugin = plugin;
        }

        public void onEnable() {
                plugin.getCommand("shop").setExecutor(this);
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String token, String args[]) {
                Player player = null;
                if (sender instanceof Player) player = (Player)player;
                if (args.length == 2 && args[0].equalsIgnoreCase("price")) {
                        if (!sender.hasPermission("simpleshop.edit")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission");
                                return true;
                        }
                        if (player == null) {
                                sender.sendMessage("Player expected");
                                return true;
                        }
                        double price = 0.0;
                        try {
                                price = Double.parseDouble(args[1]);
                        } catch (NumberFormatException nfe) {
                                player.sendMessage("" + ChatColor.RED + "Number expected");
                                return true;
                        }
                        if (price < 0.0) {
                                player.sendMessage("" + ChatColor.RED + "Price must be positive");
                                return true;
                        }
                        plugin.getPriceMap().put(player.getName(), price);
                        player.sendMessage("" + ChatColor.RED + "Open one of your shop chest to set its price to " + plugin.getEconomy().format(price) + ".");
                } else if (args.length == 1 && args[0].equals("import")) {
                        if (!sender.hasPermission("simpleshop.import")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission");
                                return true;
                        }
                        LogImporter task = new LogImporter(plugin);
                        task.start();
                        sender.sendMessage("import started");
                } else {
                        sender.sendMessage("" + ChatColor.GREEN + "SimpleShop help");
                        sender.sendMessage(ChatColor.WHITE + "/shop price [price] " + ChatColor.GREEN + "Set the price");
                }
                return true;
        }
}
