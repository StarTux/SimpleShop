package com.winthier.simpleshop.sql;

import com.winthier.libsql.PluginSQLRequest;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.event.SimpleShopEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class LogTransactionRequest extends PluginSQLRequest {
        private Timestamp timestamp;
        private String shopType;
        private String player, owner;
        private double price;
        private ItemStack item;
        private String world;
        private int x, y, z;

        LogTransactionRequest(SimpleShopPlugin plugin, SimpleShopEvent event) {
                super(plugin);
                shopType = event.getShopChest().isSellingChest() ? "buy" : "sell";
                item = event.getItem();
                player = event.getPlayer().getName();
                if (event.getShopChest().hasOwner()) owner = event.getShopChest().getOwnerName();
                world = event.getShopChest().getLocation().getWorld().getName();
                x = event.getShopChest().getLocation().getBlockX();
                y = event.getShopChest().getLocation().getBlockY();
                z = event.getShopChest().getLocation().getBlockZ();
                price = event.getPrice();
        }

        LogTransactionRequest(SimpleShopPlugin plugin, Date timestamp, String shopType, String player, String owner, double price, ItemStack item, String world, int x, int y, int z) {
                super(plugin);
                this.timestamp = new Timestamp(timestamp.getTime());
                this.shopType = shopType;
                this.player = player;
                this.owner = owner;
                this.price = price;
                this.item = item;
                this.world = world;
                this.x = x;
                this.y = y;
                this.z = z;
        }

        @Override
        public void execute(Connection c) throws SQLException {
                PreparedStatement s;
                String displayName = null;
                List<Map.Entry<Enchantment, Integer>> enchantments = new ArrayList<Map.Entry<Enchantment, Integer>>();
                String lore = null;
                ItemMeta meta = item.getItemMeta();
                if (world.length() > 32) world = world.substring(0, 32);
                if (meta.hasDisplayName()) {
                        displayName = meta.getDisplayName();
                        if (displayName.length() > 32) displayName = displayName.substring(0, 32);
                }
                if (meta instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta esmeta = (EnchantmentStorageMeta)meta;
                        if (esmeta.hasStoredEnchants()) {
                                enchantments.addAll(esmeta.getStoredEnchants().entrySet());
                        }
                } else {
                        if (meta.hasEnchants()) {
                                enchantments.addAll(meta.getEnchants().entrySet());
                        }
                }
                if (meta.hasLore()) {
                        List<String> tmp = meta.getLore();
                        if (!tmp.isEmpty()) {
                                StringBuilder sb = new StringBuilder(tmp.get(0));
                                for (int i = 1; i < tmp.size(); ++i) {
                                        sb.append("\n").append(tmp.get(i));
                                }
                                lore = sb.toString();
                                if (lore.length() > 256) lore = lore.substring(0, 256);
                        }
                }
                StringBuilder sb = new StringBuilder();
                // build string
                sb.append("INSERT INTO simpleshop_transactions SET");
                if (timestamp != null) sb.append(" time=?,");
                sb.append(" shop_type=?,");
                sb.append(" player=?,");
                if (owner != null) sb.append(" owner=?,");
                sb.append(" price=?,");
                sb.append(" itemid=?,");
                sb.append(" amount=?,");
                sb.append(" itemdata=?,");
                if (displayName != null) sb.append(" display_name=?,");
                for (Map.Entry<Enchantment, Integer> entry : enchantments) {
                        sb.append(" enchantment_");
                        sb.append(entry.getKey().getName().toLowerCase());
                        sb.append("=?,");
                }
                if (lore != null) {
                        sb.append(" lore=?,");
                }
                sb.append(" world=?,");
                sb.append(" x=?,");
                sb.append(" y=?,");
                sb.append(" z=?");
                // statement
                s = c.prepareStatement(sb.toString());
                // values
                int i = 1;
                if (timestamp != null) s.setTimestamp(i++, timestamp);
                s.setString(i++, shopType);
                s.setString(i++, player);
                if (owner != null) s.setString(i++, owner);
                s.setDouble(i++, price);
                s.setInt(i++, item.getTypeId());
                s.setInt(i++, item.getAmount());
                s.setInt(i++, item.getDurability());
                if (displayName != null) s.setString(i++, displayName);
                for (Map.Entry<Enchantment, Integer> entry : enchantments) {
                        s.setInt(i++, entry.getValue());
                }
                if (lore != null) s.setString(i++, lore);
                s.setString(i++, world);
                s.setInt(i++, x);
                s.setInt(i++, y);
                s.setInt(i++, z);
                s.execute();
                s.close();
        }
}
