package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
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
                        Util.sendMessage(sender, "&3* &lSimpleShop help&3 *");
                        if (plugin.allowShopSigns()) {
                                Util.sendMessage(sender, "&3/shop &bprice [&oprice&b]&r - Set the price");
                        }
                        if (plugin.sqlManager != null) {
                                Util.sendMessage(sender, "&3/shop &blist [&opage&b]&r - List sales");
                                Util.sendMessage(sender, "&3/shop &bavg [&odays&b]&r - Get average price of item in hand");
                        }
                        return true;
                } else if (args.length == 2 && args[0].equals("price")) {
                        if (!sender.hasPermission("simpleshop.edit")) {
                                Util.sendMessage(sender, "&cYou don't have permission");
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
                                Util.sendMessage(player, "&cNumber expected");
                                return true;
                        }
                        if (price < 0.0) {
                                Util.sendMessage(player, "&cPrice must be positive");
                                return true;
                        }
                        plugin.getPriceMap().put(player.getName(), price);
                        Util.sendMessage(player, "&cOpen one of your shop chest to set its price to .", plugin.formatPrice(price));
                        return true;
                } else if ((args.length >= 1 || args.length <= 3) && args[0].equals("list")) {
                        if (plugin.sqlManager == null) return false;
                        if (!sender.hasPermission("simpleshop.list.self")) {
                                Util.sendMessage(sender, "&cYou don't have permission");
                                return true;
                        }
                        int page = 0;
                        if (args.length == 2) {
                                try {
                                        page = Integer.parseInt(args[1]) - 1;
                                        if (page < 0) throw new NumberFormatException();
                                } catch (NumberFormatException e) {
                                        Util.sendMessage(sender, "&cPage number expected: %s.", args[1]);
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
                                if (days <= 0 || days > 9999) {
                                        Util.sendMessage(sender, "&cNumber of days expected: %s.", args[1]);
                                        return true;
                                }
                        }
                        plugin.sqlManager.sendAveragePrice(sender, item, days);
                        return true;
                } else if (args.length == 1 && args[0].equals("update")) {
                        if (!sender.hasPermission("simpleshop.admin")) {
                                Util.sendMessage(sender, "&cYou don't have permission");
                                return true;
                        }
                        plugin.sqlManager.updateTable();
                        Util.sendMessage(sender, "&bTable updated. See console.");
                        return true;
                }
                return false;
        }
}
