package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
                        Util.sendMessage(sender, "&e* &lSimpleShop help&e *");
                        Util.sendMessage(sender, "&e/shop &6price [price]&r - Set the price");
                        if (plugin.sqlManager != null) {
                                Util.sendMessage(sender, "&e/shop &6list [page]&r - List sales");
                                Util.sendMessage(sender, "&e/shop &6avg&r - Get average price of item in hand");
                        }
                        return true;
                } else if (args.length == 2 && args[0].equals("price")) {
                        if (!sender.hasPermission("simpleshop.edit")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission");
                                return true;
                        }
                        if (player == null) {
                                Util.sendMessage(sender, "&cPlayer expected.");
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
                        sender.sendMessage("import started");
                        return true;
                } else if ((args.length >= 1 || args.length <= 3) && args[0].equals("list")) {
                        if (plugin.sqlManager == null) return false;
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
                        if (player != null) plugin.sqlManager.listTransactions(sender, player.getName(), page);
                        else plugin.sqlManager.listTransactions(sender, SimpleShopPlugin.getAdminShopName(), page);
                        return true;
                } else if (args.length <= 2 && (args[0].equals("avg") || args[0].equals("average"))) {
                        if (plugin.sqlManager == null) return false;
                        if (player == null) {
                                Util.sendMessage(sender, "&cPlayer expected.");
                                return true;
                        }
                        ItemStack item = player.getItemInHand();
                        if (item == null || item.getType() == Material.AIR) {
                                Util.sendMessage(sender, "&cHold the item to check in your hand.");
                                return true;
                        }
                        int days = 90;
                        if (args.length >= 2) {
                                try {
                                        days = Integer.parseInt(args[1]);
                                } catch (NumberFormatException nfe) {
                                        days = 0;
                                }
                                if (days <= 0) {
                                        Util.sendMessage(sender, "&cPositive number expected: %s.", args[1]);
                                        return true;
                                }
                        }
                        plugin.sqlManager.sendAveragePrice(sender, item, days);
                        return true;
                }
                return false;
        }
}
