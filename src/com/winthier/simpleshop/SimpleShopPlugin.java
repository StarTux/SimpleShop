package com.winthier.simpleshop;

import com.winthier.simpleshop.ShopChest;
import com.winthier.simpleshop.listener.CommandListener;
import com.winthier.simpleshop.listener.PlayerListener;
import com.winthier.simpleshop.listener.SignListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.item.Items;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleShopPlugin extends JavaPlugin {
        private final PlayerListener playerListener = new PlayerListener(this);
        private final SignListener signListener = new SignListener(this);
        private final CommandListener commandListener = new CommandListener(this);
        private Economy economy;
        private Map<String, Double> priceMap = new HashMap<String, Double>();
        private PrintStream simpleShopLog;

        @Override
        public void onEnable() {
                if (!setupEconomy()) {
                        getLogger().warning("Failed to setup economy. SimpleShop is not enabled.");
                        setEnabled(false);
                        return;
                }
                playerListener.onEnable();
                signListener.onEnable();
                commandListener.onEnable();
                try {
                        getDataFolder().mkdirs();
                        simpleShopLog = new PrintStream(new FileOutputStream(new File(getDataFolder(), "SimpleShop.log"), true));
                } catch (FileNotFoundException fnfe) {
                        getLogger().warning("Could not open `SimpleShop.log' for writing: " + fnfe.getMessage());
                }
        }

        @Override
        public void onDisable() {
                if (simpleShopLog != null) simpleShopLog.close();
        }

        private boolean setupEconomy()
        {
                RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
                if (economyProvider != null) {
                        economy = economyProvider.getProvider();
                }
                return (economy != null);
        }

        public Economy getEconomy() {
                return economy;
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

        public void logSale(ShopChest shop, String user, ItemStack item, double price) {
                if (simpleShopLog == null) return;
                final String time = getTimeString();
                final String shopType = shop.isSellingChest() ? "buy" : "sell";
                final String shopDirection = shop.isSellingChest() ? "from" : "to";
                final Location loc = shop.getLocation();
                simpleShopLog.format("%s [%s] shop='%s' user='%s' item='%dx%d:%d' price='%.02f' location='%s:%d,%d,%d'\n", time, shopType, shop.getOwnerName(), user, item.getAmount(), item.getTypeId(), item.getDurability(), price, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                getLogger().info(String.format("[%s] '%s' %dx%d:%d for %.02f %s %s at %s:%d,%d,%d", shopType, user, item.getAmount(), item.getTypeId(), item.getDurability(), price, shopDirection, shop.getOwnerName(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }
}
