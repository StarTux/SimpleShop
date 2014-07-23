package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.ShopType;
import com.winthier.simpleshop.Util;
import com.winthier.simpleshop.event.SimpleShopEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LogOfferRequest implements SQLRequest {
    private final List<Offer> offers;

    LogOfferRequest(List<Offer> offers) {
        this.offers = offers;
    }

    @Override
    public void execute(Connection c) throws SQLException {
        Statement tmp = c.createStatement();
        ResultSet result = tmp.executeQuery("(SELECT `version` FROM `simpleshop_version` WHERE `name` = 'offers')");
        result.next();
        final int version = result.getInt("version") + 1;

        PreparedStatement s;
        StringBuilder sb = new StringBuilder();

        sb.append(" INSERT INTO `simpleshop_offers`");
        sb.append(" (`version`, `shop_type`, `owner`, `world`, `x`, `y`, `z`, `material`, `amount`, `itemdata`, `display_name`, `price`, `description`)");
        sb.append(" VALUES ");
        for (int i = 0; i < offers.size(); ++i) {
            if (i > 0) sb.append(", ");
            sb.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        }

        s = c.prepareStatement(sb.toString());

        int i = 1;
        for (Offer offer : offers) {
            s.setInt(i++, version);
            s.setString(i++, offer.shopType.toString());
            s.setString(i++, offer.owner);
            s.setString(i++, offer.location.getWorld().getName());
            s.setInt(i++, offer.location.getBlockX());
            s.setInt(i++, offer.location.getBlockY());
            s.setInt(i++, offer.location.getBlockZ());
            s.setString(i++, offer.item.getType().name().toLowerCase());
            s.setInt(i++, offer.item.getAmount());
            s.setInt(i++, offer.item.getDurability());
            {
                ItemMeta meta = offer.item.getItemMeta();
                if (meta.hasDisplayName()) {
                    s.setString(i++, Util.truncate(meta.getDisplayName(), 32));
                } else {
                    s.setString(i++, null);
                }
            }
            s.setDouble(i++, offer.price);
            s.setString(i++, offer.description);
        }
        s.execute();
        s.close();
    }

    public static class Offer {
        public final ShopType shopType;
        public final String owner;
        public final Location location;
        public final ItemStack item;
        public final double price;
        public final String description;
        public Offer(ShopType shopType, String owner, Location location, ItemStack item, double price, String description) {
            if (description.length() > 255) {
                System.err.println("[SimpleShop] Offer description too long: " + description);
                description = description.substring(0, 255);
            }
            this.shopType = shopType;
            this.owner = owner;
            this.location = location;
            this.item = item;
            this.price = price;
            this.description = description;
        }
    }
}
