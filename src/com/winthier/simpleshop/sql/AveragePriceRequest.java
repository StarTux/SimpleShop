package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class AveragePriceRequest extends BukkitRunnable implements SQLRequest {
        private final SimpleShopPlugin plugin;
        private final CommandSender sender;
        private final ItemStack item;
        private final int days;

        // result
        private double price = 0.0;
        private int amount;
        private int sampleSize = 0;

        public AveragePriceRequest(SimpleShopPlugin plugin, CommandSender sender, ItemStack item, int days) {
                this.plugin = plugin;
                this.sender = sender;
                this.item = item.clone();
                this.days = days;
        }

        @Override
        public void execute(Connection c) throws SQLException {
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT COUNT(*) AS sample, sum(`price`) AS `price`, sum(`amount`) AS `amount` FROM `simpleshop_transactions`");
                sb.append(" WHERE `itemid` = ").append(item.getTypeId());
                sb.append(" AND `itemdata` = ").append((int)item.getDurability());
                sb.append(" AND `time` > DATE_SUB(NOW(), INTERVAL ").append(days).append(" DAY)");
                Map<Enchantment, Integer> enchantments = null;
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta emeta = (EnchantmentStorageMeta)meta;
                        enchantments = emeta.getStoredEnchants();
                } else {
                        enchantments = meta.getEnchants();
                }
                if (enchantments != null) {
                        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                                sb.append(" AND `enchantment_").append(entry.getKey().getName().toLowerCase());
                                sb.append("` = ").append(entry.getValue());
                        }
                }
                Statement s = c.createStatement();
                ResultSet result = s.executeQuery(sb.toString());
                if (result.next()) {
                        this.price = result.getDouble("price");
                        this.amount = result.getInt("amount");
                        this.sampleSize = result.getInt("sample");
                }
                runTask(plugin);
        }

        @Override
        public void run() {
                StringBuilder name = new StringBuilder(plugin.getItemName(item));
                ItemMeta meta = item.getItemMeta();
                Map<Enchantment, Integer> enchantments = null;
                if (meta instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta emeta = (EnchantmentStorageMeta)meta;
                        enchantments = emeta.getStoredEnchants();
                } else {
                        enchantments = meta.getEnchants();
                }
                if (enchantments != null) {
                        int i = 0;
                        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                                name.append(", ");
                                name.append(ListTransactionsRequest.getEnchantmentName(entry.getKey()));
                                name.append(" ").append(entry.getValue());
                        }
                }
                if (sampleSize == 0) {
                        Util.sendMessage(sender, "&3There are no samples for &b%s&3 over the last &b%d&3 days.", name, days);
                        return;
                }
                double perItem = 0.0;
                double perStack = 0.0;
                int stackSize = item.getMaxStackSize();
                if (amount > 0) {
                        perItem = price / (double)amount;
                        perStack = price * (double)stackSize / (double)amount;
                }
                Util.sendMessage(sender, "&3Average price of &b%s&3 from &b%d&3 samples over the last &b%d&3 days:", name.toString(), sampleSize, days);
                Util.sendMessage(sender, "&b* &f%s&b per item.", plugin.formatPrice(perItem));
                if (stackSize > 1) {
                        Util.sendMessage(sender, "&b* &f%s&b per stack (&3%d&b).", plugin.formatPrice(perStack), stackSize);
                }
        }
}
