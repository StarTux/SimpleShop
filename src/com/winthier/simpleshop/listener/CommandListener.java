package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.sql.LogImporter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.winthier.simpleshop.sql.ListTransactionsRequest;

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
                if (sender instanceof Player) player = (Player)sender;
                if (args.length == 0) {
                        sender.sendMessage("" + ChatColor.GREEN + "SimpleShop help");
                        sender.sendMessage(ChatColor.WHITE + "/shop price [price] " + ChatColor.GREEN + "- Set the price");
                        if (plugin.sqlLogger != null) {
                                sender.sendMessage(ChatColor.WHITE + "/shop list [page] " + ChatColor.GREEN + "- List sales");
                        }
                        return true;
                } else if (args.length == 2 && args[0].equals("price")) {
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
                        return true;
                } else if (args.length == 1 && args[0].equals("import")) {
                        if (!sender.hasPermission("simpleshop.import")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission");
                                return true;
                        }
                        LogImporter task = new LogImporter(plugin);
                        task.start();
                        sender.sendMessage("import started");
                        return true;
                } else if ((args.length >= 1 || args.length <= 3) && args[0].equals("list")) {
                        if (!sender.hasPermission("simpleshop.list.self")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission");
                                return true;
                        }
                        int page = 0;
                        if (args.length == 2) {
                                try {
                                        page = Integer.parseInt(args[1]) - 1;
                                        if (page < 0) throw new NumberFormatException();
                                } catch (NumberFormatException e) {
                                        sender.sendMessage("" + ChatColor.RED + "Page number expected: " + args[1]);
                                }
                        }
                        if (player != null) listTransactions(sender, player.getName(), page);
                        else listTransactions(sender, SimpleShopPlugin.getAdminShopName(), page);
                        return true;
                }
                return false;
        }

        public void listTransactions(CommandSender sender, String name, int page) {
                ListTransactionsRequest request = new ListTransactionsRequest(plugin, sender, name, page);
                plugin.sqlLogger.connectionManager.queueRequest(request);
        }
}
