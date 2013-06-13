package com.winthier.simpleshop.sql;

import com.winthier.simpleshop.SimpleShopPlugin;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.Throwable;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class LogImporter extends BukkitRunnable {
        private SimpleShopPlugin plugin;

        public LogImporter(SimpleShopPlugin plugin) {
                this.plugin = plugin;
        }

        @Override
        public void run() {
                try {
                        plugin.getLogger().info("commencing import");
                        Pattern pattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2}) \\[(buy|sell)\\] shop='([^']+)' user='([^']+)' item='(\\d+)x(\\d+):(\\d+)' price='([^']+)' location='([^:]+):([^,]+),([^,]+),([^']+)'");
                        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(plugin.getDataFolder(), "SimpleShop.log"))));
                        String line;
                        while (null != (line = in.readLine())) {
                                Matcher matcher = pattern.matcher(line);
                                if (!matcher.matches()) {
                                        plugin.getLogger().warning("No match: " + line);
                                        continue;
                                }
                                int i = 1;
                                int year = Integer.parseInt(matcher.group(i++));
                                int month = Integer.parseInt(matcher.group(i++));
                                int day = Integer.parseInt(matcher.group(i++));
                                int hour = Integer.parseInt(matcher.group(i++));
                                int minute = Integer.parseInt(matcher.group(i++));
                                int second = Integer.parseInt(matcher.group(i++));
                                String shopType = matcher.group(i++);
                                String owner = matcher.group(i++);
                                String player = matcher.group(i++);
                                int amount = Integer.parseInt(matcher.group(i++));
                                int itemid = Integer.parseInt(matcher.group(i++));
                                int itemdata = Integer.parseInt(matcher.group(i++));
                                double price = Double.parseDouble(matcher.group(i++));
                                String world = matcher.group(i++);
                                int x = Integer.parseInt(matcher.group(i++));
                                int y = Integer.parseInt(matcher.group(i++));
                                int z = Integer.parseInt(matcher.group(i++));
                                Calendar cal = Calendar.getInstance();
                                cal.set(year, month - 1, day, hour, minute, second);
                                ItemStack item = new ItemStack(itemid, amount, (short)itemdata);
                                LogTransactionRequest request;
                                request = new LogTransactionRequest(plugin, cal.getTime(), shopType, player, owner, price, item, world, x, y, z);
                                while (!plugin.sqlLogger.connectionManager.queueRequest(request)) {
                                        plugin.getLogger().info("queue full. sleeping 1 second...");
                                        Thread.sleep(1000);
                                }
                        }
                        plugin.getLogger().info("import done");
                } catch (Throwable t) {
                        t.printStackTrace();
                }
        }

        public void start() {
                runTaskAsynchronously(plugin);
        }
}
