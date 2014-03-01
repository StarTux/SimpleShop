package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.ShopType;
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

public class LogOfferRequest implements SQLRequest {
        private final ShopType shopType;
        private final String owner;
        private final Location location;
        private final int amount;
        private final double price;
        private final String description;

        LogOfferRequest(ShopType shopType, String owner, Location location, int amount, double price, String description) {
                this.shopType = shopType;
                this.owner = owner;
                this.location = location;
                this.amount = amount;
                this.price = price;
                if (description.length() > 255) {
                        System.err.println("[SimpleShop] Offer description too long: " + description);
                        description = description.substring(0, 255);
                }
                this.description = description;
        }

        @Override
        public void execute(Connection c) throws SQLException {
                PreparedStatement s;
                s = c.prepareStatement(" INSERT INTO `simpleshop_offers`" +
                                       " (`version`, `shop_type`, `owner`, `world`, `x`, `y`, `z`, `amount`, `price`, `description`)" +
                                       " VALUES ((SELECT `version` FROM `simpleshop_version` WHERE `name` = 'offers') + 1," +
                                       " ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                int i = 1;
                s.setString(i++, shopType.toString());
                s.setString(i++, owner);
                s.setString(i++, location.getWorld().getName());
                s.setInt(i++, location.getBlockX());
                s.setInt(i++, location.getBlockY());
                s.setInt(i++, location.getBlockZ());
                s.setInt(i++, amount);
                s.setDouble(i++, price);
                s.setString(i++, description);
                s.execute();
                s.close();
        }
}
