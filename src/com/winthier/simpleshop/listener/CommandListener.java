package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                if (!handleCommand(sender, command, token, args)) {
                        Util.sendMessage(sender, "&b&lSimpleShop Help");
                        if (plugin.allowShopSigns()) {
                                Util.sendMessage(sender, "&3/Shop &bPrice [&oprice&b]&8 - &7Set the price");
                        }
                        if (plugin.sqlManager != null) {
                                Util.sendMessage(sender, "&3/Shop &bList [&opage&b]&8 - &7List sales");
                                Util.sendMessage(sender, "&3/Shop &bAvg [&odays&b]&8 - &7Get average price of item in hand");
                                Util.sendMessage(sender, "&3/Shop &bPlayerStats [&odays&b] [&opage&b]&8 - &7View customer statistics");
                                Util.sendMessage(sender, "&3/Shop &bItemStats [&odays&b] [&opage&b]&8 - &7View sales statistics");
                                if (plugin.marketCrawler != null) {
                                        Util.sendMessage(sender, "&3/Shop &bSearch [&oflags&b] <&okeyword&b...>&8 - &7Search the market");
                                        Util.sendMessage(sender, "&3&o Flags: &b-f&3 Use whole keyword &b-e&3 Only exact matches");
                                }
                        }
                }
                return true;
        }

        private boolean handleCommand(CommandSender sender, Command command, String token, String args[]) {
                Player player = null;
                if (sender instanceof Player) player = (Player)sender;
                if (args.length == 0) {
                        return false;
                } else if (args.length == 2 && "Price".equalsIgnoreCase(args[0])) {
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
                } else if ((args.length >= 1 || args.length <= 3) && "List".equalsIgnoreCase(args[0])) {
                        if (plugin.sqlManager == null) return false;
                        if (!sender.hasPermission("simpleshop.list")) {
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
                } else if (args.length <= 2 && ("Avg".equalsIgnoreCase(args[0]) || "Average".equalsIgnoreCase(args[0]))) {
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
                } else if (args.length >= 1 && args.length <= 3 && ("PlayerStats".equalsIgnoreCase(args[0]) || "ItemStats".equalsIgnoreCase(args[0]))) {
                        if (plugin.sqlManager == null) return false;
                        if (player == null) {
                                Util.sendMessage(sender, "&cPlayer expected.");
                                return true;
                        }
                        if (!sender.hasPermission("simpleshop.stats")) {
                                Util.sendMessage(sender, "&cYou don't have permission");
                                return true;
                        }
                        int days = 90;
                        if (args.length >= 2) {
                                try {
                                        days = Integer.parseInt(args[1]);
                                } catch (NumberFormatException nfe) {
                                        days = 0;
                                }
                                if (days < 1) {
                                        Util.sendMessage(sender, "&cInvalid number of days: %s.", args[1]);
                                        return true;
                                }
                        }
                        int page = 0;
                        if (args.length >= 3) {
                                try {
                                        page = Integer.parseInt(args[2]) - 1;
                                } catch (NumberFormatException nfe) {
                                        page = -1;
                                }
                                if (page < 0) {
                                        Util.sendMessage(sender, "&cInvalid page number: %s.", args[2]);
                                        return true;
                                }
                        }
                        if ("PlayerStats".equalsIgnoreCase(args[0])) {
                                plugin.sqlManager.sendShopPlayerStatistics(sender, sender.getName(), days, page);
                        } else if ("ItemStats".equalsIgnoreCase(args[0])) {
                                plugin.sqlManager.sendShopItemStatistics(sender, sender.getName(), days, page);
                        }
                        return true;
                } else if (args.length >= 2 && "Search".equalsIgnoreCase(args[0])) {
                        if (plugin.sqlManager == null) return false;
                        if (!sender.hasPermission("simpleshop.search")) {
                                Util.sendMessage(sender, "&cYou don't have permission");
                                return true;
                        }

                        List<String> items = new ArrayList<String>();
                        boolean exact = false;
                        boolean full = false;

                        // The -e flag requires the search item to match the keywords exactly.
                        // The -f flag turns the whole remaining command line into one keyword.
                        if (args[1].length() > 1 && args[1].startsWith("-")) {
                                for (int i = 1; i < args[1].length(); ++i) {
                                        if (args[1].charAt(i) == 'e') exact = true;
                                        if (args[1].charAt(i) == 'f') full = true;
                                }
                        }

                        // Check intput
                        Pattern pattern = Pattern.compile("[A-Za-z0-9-_,.]+");
                        for (int i = 1; i < args.length; ++i) {
                                if (!pattern.matcher(args[i]).matches()) {
                                        Util.sendMessage(sender, "&cBad keyword: %s.", args[i]);
                                        return true;
                                }
                        }

                        // If any of the flags are present, the keywords have to be combined.
                        if (exact || full) {
                                if (args.length < 3) {
                                        Util.sendMessage(sender, "&cKeywords missing.");
                                        return true;
                                }
                                StringBuilder sb = new StringBuilder(args[2]);
                                for (int i = 3; i < args.length; ++i) sb.append(" ").append(args[i]);
                                items.add(sb.toString());
                        } else {
                                for (int i = 1; i < args.length; ++i) items.add(args[i]);
                        }
                        plugin.sqlManager.searchOffers(sender, items, exact);
                        return true;
                } else if (args.length == 1 && "More".equalsIgnoreCase(args[0])) {
                        if (player == null) {
                                Util.sendMessage(sender, "&cPlayer expected.");
                                return true;
                        }
                        String[] messages = plugin.messageStore.getPage(player);
                        if (messages == null) {
                                Util.sendMessage(sender, "&cNo further messages.");
                                return true;
                        }
                        for (String msg : messages) {
                                sender.sendMessage(msg);
                        }
                        return true;
                } else if (args.length == 1 && "Update".equals(args[0])) {
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
