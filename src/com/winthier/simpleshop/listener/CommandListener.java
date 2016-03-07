package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.MarketCrawler;
import com.winthier.simpleshop.ShopType;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
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
        boolean result = true;
        try {
            result = handleCommand(sender, command, token, args);
        } catch (CommandException ce) {
            Util.sendMessage(sender, "&c%s", ce.getMessage());
        }
        if (!result) {
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
                    Util.sendMessage(sender, "&3/Shop &bSearch [&okeyword&b...]&8 - &7Search for items");
                    Util.sendMessage(sender, "&3/Shop &bSearch! [&okeyword&b...]&8 - &7Search exact items");
                    Util.sendMessage(sender, "&3/Shop &bPort [&oowner&b]&8 - &7Port to someone's shop");
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
            ItemStack item = player.getInventory().getItemInMainHand();
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
        } else if (args.length >= 1 && ("Search".equalsIgnoreCase(args[0]) || "Search!".equalsIgnoreCase(args[0]))) {
            if (player == null) throw new CommandException("Player expected");
            if (plugin.sqlManager == null) return false;
            if (!sender.hasPermission("simpleshop.search")) throw new CommandException("You don't have permission");

            boolean exact = false;
            if ("Search!".equalsIgnoreCase(args[0])) exact = true;

            if (args.length < 2) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) throw new CommandException("Hold an item in your hand or provide search keywords");
                String desc = MarketCrawler.getItemDescription(item);
                List<String> list = new ArrayList<>(1);
                list.add(desc);
                plugin.sqlManager.searchOffers(player, ShopType.BUY, list, exact);
                return true;
            }

            List<String> items = new ArrayList<String>();
            // Check intput
            Pattern pattern = Pattern.compile("[A-Za-z0-9-_,.]+");
            for (int i = 1; i < args.length; ++i) {
                if (args[i].length() == 0) throw new CommandException("Empty keyword #" + i);
                if (!pattern.matcher(args[i]).matches()) throw new CommandException("Bad keyword: " + args[i]);
            }

            // If any of the flags are present, the keywords have to be combined.
            if (exact) {
                StringBuilder sb = new StringBuilder(args[1]);
                for (int i = 2; i < args.length; ++i) sb.append(" ").append(args[i]);
                items.add(sb.toString());
            } else {
                for (int i = 1; i < args.length; ++i) {
                    String keyword = args[i];
                    if (keyword.length() > 3 && keyword.endsWith("s")) {
                        keyword = keyword.substring(0, keyword.length() - 1);
                    }
                    items.add(keyword);
                }
            }
            plugin.sqlManager.searchOffers(player, ShopType.BUY, items, exact);
            return true;
        } else if (args.length == 1 && "More".equalsIgnoreCase(args[0])) {
            if (player == null) {
                Util.sendMessage(sender, "&cPlayer expected.");
                return true;
            }
            Object[] messages = plugin.messageStore.getPage(player);
            if (messages == null) {
                Util.sendMessage(sender, "&cNo further messages.");
                return true;
            }
            for (Object o : messages) {
                Util.tellRaw(player, o);
            }
            return true;
        } else if ((args.length == 1 || args.length == 2) && "Port".equalsIgnoreCase(args[0])) {
            if (player == null) {
                Util.sendMessage(sender, "&cPlayer expected.");
                return true;
            }
            String ownerName = null;
            if (args.length == 1) {
                ownerName = player.getName();
            } else {
                ownerName = args[1];
            }
            plugin.sqlManager.portShop(player, ownerName);
            return true;
        }
        // } else if (args.length == 1 && "Update".equals(args[0])) {
        //     if (!sender.hasPermission("simpleshop.admin")) {
        //         Util.sendMessage(sender, "&cYou don't have permission");
        //         return true;
        //     }
        //     plugin.sqlManager.updateTable();
        //     Util.sendMessage(sender, "&bTable updated. See console.");
        //     return true;
        // }
        return false;
    }
}
