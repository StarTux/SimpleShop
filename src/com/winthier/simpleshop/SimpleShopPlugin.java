package com.winthier.simpleshop;

import com.winthier.simpleshop.ShopChest;
import com.winthier.simpleshop.listener.CommandListener;
import com.winthier.simpleshop.listener.PlayerListener;
import com.winthier.simpleshop.listener.SignListener;
import com.winthier.simpleshop.sql.SQLManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleShopPlugin extends JavaPlugin {
        private final PlayerListener playerListener = new PlayerListener(this);
        private SignListener signListener;
        private final CommandListener commandListener = new CommandListener(this);
        private Economy economy;
        private Map<String, Double> priceMap = new HashMap<String, Double>();
        private Map<String, Double> paidMap = new HashMap<String, Double>();
        // configuration
        private static SimpleShopPlugin instance;
        private boolean allowShopSigns, useItemEconomy;
        private String shopCode, sellingCode, buyingCode, adminShopName;
        private Map<MaterialData, Double> itemCurrency = new HashMap<MaterialData, Double>();
        private String currencyName;
        // sql
        public SQLManager sqlManager;

        @Override
        public void onEnable() {
                this.instance = this;
                // load config
                reloadConfig();
                allowShopSigns = getConfig().getBoolean("AllowShopSigns", true);
                shopCode = getConfig().getString("ShopCode");
                sellingCode = getConfig().getString("SellingCode");
                buyingCode = getConfig().getString("BuyingCode");
                adminShopName = getConfig().getString("AdminShopName");
                useItemEconomy = getConfig().getBoolean("UseItemEconomy");
                // setup economy
                if (useItemEconomy) {
                        ConfigurationSection currencySection = getConfig().getConfigurationSection("currency");
                        for (String key : currencySection.getKeys(false)) {
                                ItemInfo info = Items.itemByString(key);
                                if (info == null) {
                                        getLogger().warning(currencySection.getCurrentPath() + ": invalid item type: " + key);
                                        continue;
                                }
                                MaterialData md = new MaterialData(info.getType(), (byte)info.getSubTypeId());
                                double value = currencySection.getDouble(key);
                                if (currencyName == null || value == 1.0) currencyName = key;
                                itemCurrency.put(md, value);
                        }
                } else {
                        if (!setupEconomy()) {
                                getLogger().warning("Failed to setup economy. SimpleShop is not enabled.");
                                setEnabled(false);
                                return;
                        }
                }
                // setup listeners
                playerListener.onEnable();
                if (allowShopSigns) {
                        signListener = new SignListener(this);
                        signListener.onEnable();
                }
                commandListener.onEnable();
                // setup sql logging
                if (getConfig().getBoolean("sql.enable")) {
                        sqlManager = new SQLManager(this);
                        sqlManager.onEnable();
                }
                getConfig().options().copyDefaults(true);
                saveConfig();
        }

        @Override
        public void onDisable() {
                if (sqlManager != null) sqlManager.onDisable();
                currencyName = null;
        }

        private boolean setupEconomy()
        {
                RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
                if (economyProvider != null) {
                        economy = economyProvider.getProvider();
                }
                return (economy != null);
        }

        public static Economy getEconomy() {
                return instance.economy;
        }

        public static boolean allowShopSigns() {
                return instance.allowShopSigns;
        }

        /**
         * Get the code for the default shop, which sells things
         * to people.
         */
        public static String getShopCode() {
                return instance.shopCode;
        }

        /**
         * Get the code for a shop which sells things to people,
         * resp. people buy things from.
         */
        public static String getSellingCode() {
                return instance.sellingCode;
        }

        /**
         * Get the code for a shop which buys things from people,
         * resp. people sell things to.
         */
        public static String getBuyingCode() {
                return instance.buyingCode;
        }

        public static String getAdminShopName() {
                return instance.adminShopName;
        }

        public static String formatPrice(double price) {
                if (instance.useItemEconomy) {
                        String tag = "" + price;
                        String tags[] = tag.split("\\.");
                        if (tags.length == 2 && tags[1].equals("0")) {
                                return "" + tags[0] + " " + instance.currencyName;
                        } else {
                                return String.format("%.02f ", price) + instance.currencyName;
                        }
                } else {
                        return instance.economy.format(price);
                }
        }

        public String getItemName(ItemStack item) {
                try {
                        return Items.itemByStack(item).getName();
                } catch (NullPointerException npe) {
                        // if vault is outdated
                        String[] parts = item.getType().name().split("_");
                        StringBuilder sb = new StringBuilder(capitalName(parts[0]));
                        for (int i = 1; i < parts.length; ++i) {
                                sb.append(" ").append(capitalName(parts[i]));
                        }
                        return sb.toString();
                }
        }

        private static String capitalName(String in) {
                return "" + Character.toUpperCase(in.charAt(0)) + in.substring(1, in.length()).toLowerCase();
        }

        public Map<String, Double> getPriceMap() {
                return priceMap;
        }

        private String getTimeString() {
                Calendar cal = GregorianCalendar.getInstance();
                return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                                     cal.get(Calendar.YEAR),
                                     cal.get(Calendar.MONTH) + 1,
                                     cal.get(Calendar.DAY_OF_MONTH),
                                     cal.get(Calendar.HOUR_OF_DAY),
                                     cal.get(Calendar.MINUTE),
                                     cal.get(Calendar.SECOND));
        }

        public static boolean useItemEconomy() {
                return instance.useItemEconomy;
        }

        public static double getCurrencyValue(MaterialData mat) {
                Double tmp = instance.itemCurrency.get(mat);
                return tmp == null ? 0.0 : tmp;
        }

        public static double addPaidItems(Player player, double amount) {
                Double tmp = instance.paidMap.get(player.getName());
                double total = tmp == null ? 0.0 : tmp;
                total += amount;
                instance.paidMap.put(player.getName(), total);
                return total;
        }

        public static double getPaidItems(Player player) {
                Double tmp = instance.paidMap.get(player.getName());
                return tmp == null ? 0.0 : instance.paidMap.get(player.getName());
        }

        public static void resetPaidItems(Player player) {
                instance.paidMap.remove(player.getName());
        }

        public static boolean hasMoney(Player player, double amount) {
                if (instance.useItemEconomy) {
                        return getPaidItems(player) >= amount;
                } else {
                        return instance.economy.has(player.getName(), amount);
                }
        }

        public static void giveMoney(Player player, double amount) {
                if (instance.useItemEconomy) {
                        addPaidItems(player, amount);
                } else {
                        instance.economy.depositPlayer(player.getName(), amount);
                }
        }

        public static boolean takeMoney(Player player, double amount) {
                if (instance.useItemEconomy) {
                        if (!hasMoney(player, amount)) return false;
                        addPaidItems(player, -amount);
                        return true;
                } else {
                        return instance.economy.withdrawPlayer(player.getName(), amount).transactionSuccess();
                }
        }

        public static Set<MaterialData> getCurrencyItems() {
                return instance.itemCurrency.keySet();
        }

        public void logSale(ShopChest shop, String user, ItemStack item, double price) {
                String itemName;
                ItemInfo info = Items.itemByStack(item);
                if (info != null) {
                        itemName = info.toString();
                } else {
                        itemName = item.getType().name().toLowerCase();
                        if (item.getDurability() != 0) itemName += ":" + item.getDurability();
                }
                final String time = getTimeString();
                final String shopType = shop.isSellingChest() ? "buy" : "sell";
                final String shopDirection = shop.isSellingChest() ? "from" : "to";
                final Location loc = shop.getLocation();
                getLogger().info(String.format("[%s] '%s' %dx%s for %.02f %s %s at %s:%d,%d,%d", shopType, user, item.getAmount(), itemName, price, shopDirection, shop.getOwnerName(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }
}
