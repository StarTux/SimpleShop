package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
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
        private final String owner;
        private final Location location;
        private final int amount;
        private final double price;
        private final String description;

        LogOfferRequest(String owner, Location location, int amount, double price, String description) {
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
                                       " (`version`, `owner`, `world`, `x`, `y`, `z`, `amount`, `price`, `description`)" +
                                       " VALUES ((SELECT `version` FROM `simpleshop_version` WHERE `name` = 'offers') + 1," +
                                       " ?, ?, ?, ?, ?, ?, ?, ?)");
                s.setString(1, owner);
                s.setString(2, location.getWorld().getName());
                s.setInt(3, location.getBlockX());
                s.setInt(4, location.getBlockY());
                s.setInt(5, location.getBlockZ());
                s.setInt(6, amount);
                s.setDouble(7, price);
                s.setString(8, description);
                s.execute();
                s.close();
        }
}
