package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SearchOffersRequest extends BukkitRunnable implements SQLRequest {
        private final SimpleShopPlugin plugin;
        private final CommandSender sender;
        private final List<String> items;
        private final int PAGE_LEN = 4;
        private final int PAGE_KEY = 0;
        private final boolean exact;
        // Result
        private final List<Offer> offers = new ArrayList<Offer>();

        SearchOffersRequest(SimpleShopPlugin plugin, CommandSender sender, List<String> items, boolean exact) {
                this.plugin = plugin;
                this.sender = sender;
                this.items = items;
                this.exact = exact;
        }

        @Override
        public void execute(Connection c) throws SQLException {
                PreparedStatement s;
                StringBuilder sb = new StringBuilder();
                sb.append(" SELECT * FROM `simpleshop_offers`");
                sb.append(" WHERE `version` = (");
                sb.append("   SELECT `version` FROM `simpleshop_version`");
                sb.append("   WHERE `name` = 'offers'");
                sb.append(" )");
                for (int i = 0; i < items.size(); ++i) {
                        if (!exact) {
                                sb.append(" AND `description` LIKE ?");
                        } else {
                                sb.append(" AND `description` = ?");
                        }
                }
                sb.append(" GROUP BY `owner`, `description`");
                sb.append(" ORDER BY `price` / `amount` DESC");
                s = c.prepareStatement(sb.toString());

                int i = 0;
                for (String item : items) {
                        if (!exact) {
                                s.setString(++i, "%" + item + "%");
                        } else {
                                s.setString(++i, item);
                        }
                }

                ResultSet result = s.executeQuery();
                while (result.next()) {
                        offers.add(new Offer(result.getString("owner"),
                                             result.getString("world"),
                                             result.getInt("x"), result.getInt("y"), result.getInt("z"),
                                             result.getInt("amount"),
                                             result.getDouble("price"),
                                             result.getString("description")));
                }
                s.close();

                runTask(plugin);
        }

        public String formatOffer(Offer offer, Player player) {
                String result = Util.format("&r%s&3 sells &b%d&3x&b%s&3 for &b%s", offer.owner, offer.amount, offer.description, plugin.formatPrice(offer.price));
                if (player != null && player.getWorld().getName().equals(offer.world)) {
                        final Location loc = player.getLocation();
                        int dx = offer.x - loc.getBlockX();
                        int dz = offer.z - loc.getBlockZ();
                        String direction = "";
                        if (dz > 4) direction = "South";
                        if (dz < -4) direction = "North";
                        if (dx > 4) direction += "East";
                        if (dx < -4) direction += "West";
                        if ("".equals(direction)) direction = "Right here";
                        result += Util.format("\n&3Location: &b%s&3 &b%d&3,&b%d&3,&b%d&3 (&b%s&3)", offer.world, offer.x, offer.y, offer.z, direction);
                } else {
                        result += Util.format("\n&3Location: &b%s&3 &b%d&3,&b%d&3,&b%d", offer.world, offer.x, offer.y, offer.z);
                }
                return result;
        }

        @Override
        public void run() {
                // Header
                final int pages = (offers.size() - 1) / PAGE_LEN + 1;
                StringBuilder sb = new StringBuilder("&3Search results for &b");
                sb.append(items.get(0));
                for (int i = 1; i < items.size(); ++i) sb.append("&3, &b").append(items.get(i));
                Util.sendMessage(sender, sb.toString());

                // First page
                final Player player = sender instanceof Player ? (Player)sender : null;
                if (player != null) plugin.messageStore.clearPages(player);
                if (offers.isEmpty()) {
                        Util.sendMessage(sender, "&cNo results.");
                        return;
                }
                final Iterator<Offer> iter = offers.iterator();
                for (int i = 0; i < PAGE_LEN && iter.hasNext(); ++i) sender.sendMessage(formatOffer(iter.next(), player));
                if (!iter.hasNext() || player == null) {
                        return;
                }

                // Message storage
                Util.sendMessage(player, "&3&m   &3 Page &b1&3/&b%d&3, Type &b/Shop More&3 for more.", pages);
                List<String> msg = new ArrayList<String>();
                int page = 1;
                while (iter.hasNext()) {
                        page += 1;
                        for (int i = 0; i < PAGE_LEN && iter.hasNext(); ++i) msg.add(formatOffer(iter.next(), player));
                        if (iter.hasNext()) {
                                msg.add(Util.format("&3&m   &3 Page &b%d&3/&b%d&3, Type &b/Shop More&3 for more.", page, pages));
                        }
                        plugin.messageStore.storePage(player, msg);
                        msg.clear();
                }
        }
}

class Offer {
        public String owner;
        public String world;
        public int x, y, z;
        public int amount;
        public double price;
        public String description;

        public Offer(String owner, String world, int x, int y, int z, int amount, double price, String description) {
                this.owner = owner;
                this.world = world;
                this.x = x;
                this.y = y;
                this.z = z;
                this.amount = amount;
                this.price = price;
                this.description = description;
        }
}
