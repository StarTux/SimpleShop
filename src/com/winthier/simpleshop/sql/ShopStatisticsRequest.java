package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class ShopStatisticsRequest extends BukkitRunnable implements SQLRequest {
        public static enum Type {
                PLAYER("player", "customer"),
                ITEM("material", "item");

                public final String field, title;

                Type(String field, String title) {
                        this.field = field;
                        this.title = title;
                }
        }

        private final SimpleShopPlugin plugin;
        private final CommandSender sender;
        private final String owner;
        private final Type type;
        private final int days;
        private final int page;
        private final int PAGE_LEN = 10;

        // Result
        private final LinkedHashMap<String, Integer> ranking = new LinkedHashMap<String, Integer>();

        public ShopStatisticsRequest(SimpleShopPlugin plugin, CommandSender sender, String owner, Type type, int days, int page) {
                this.plugin = plugin;
                this.sender = sender;
                this.owner = owner;
                this.type = type;
                this.days = days;
                this.page = page;
        }

        @Override
        public void execute(Connection c) throws SQLException {
                PreparedStatement s;
                s = c.prepareStatement(" SELECT `" + type.field + "` AS `type`, SUM(`price`) AS `amount`" +
                                       " FROM `simpleshop_transactions`" +
                                       " WHERE `owner` = ?" +
                                       " AND `shop_type` = 'buy'" +
                                       " AND `time` > DATE_SUB(NOW(), INTERVAL ? DAY)" +
                                       " GROUP BY `" + type.field + "`" +
                                       " ORDER BY `amount` DESC" +
                                       " LIMIT ?, ?");
                s.setString(1, owner);
                s.setInt(2, days);
                s.setInt(3, page * PAGE_LEN);
                s.setInt(4, PAGE_LEN);

                ResultSet result = s.executeQuery();
                while (result.next()) {
                        ranking.put(result.getString("type"), result.getInt("amount"));
                }
                s.close();

                runTask(plugin);
        }

        @Override
        public void run() {
                int rank = page * PAGE_LEN;
                Util.sendMessage(sender, "&b%s&3 %s statistics from the last &b%d&3 days (page &b%s&3).", Util.genitiveName(owner), type.title, days, (page + 1));
                for (String key : ranking.keySet()) {
                        String name = key;
                        switch (type) {
                        case ITEM:
                                final Material mat = Material.matchMaterial(key);
                                if (mat != null) name = Util.getItemName(mat);
                        default:
                        }
                        String amount = plugin.formatPrice(ranking.get(key));
                        Util.sendMessage(sender, " &b%02d &3%s &b%s", ++rank, amount, name);
                }
        }
}
