package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.ShopType;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SearchOffersRequest extends BukkitRunnable implements SQLRequest {
    private final SimpleShopPlugin plugin;
    private final Player player;
    private final ShopType shopType;
    private final List<String> items;
    private final int PAGE_LEN = 4;
    private final int PAGE_KEY = 0;
    private final boolean exact;
    // Result
    private final List<Offer> offers = new ArrayList<Offer>();

    SearchOffersRequest(SimpleShopPlugin plugin, Player player, ShopType shopType, List<String> items, boolean exact) {
        this.plugin = plugin;
        this.player = player;
        this.shopType = shopType;
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
        sb.append(" AND `shop_type` = ?");
        for (int i = 0; i < items.size(); ++i) {
            if (!exact) {
                sb.append(" AND `description` LIKE ?");
            } else {
                sb.append(" AND `description` = ?");
            }
        }
        sb.append(" GROUP BY `owner`, (`price`/`amount`), `description`");
        sb.append(" ORDER BY `price` / `amount` ASC");
        s = c.prepareStatement(sb.toString());

        int i = 0;
        s.setString(++i, shopType.toString());
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

    public List<Object> formatOffer(Offer offer, Player player) {
        List<Object> result = new ArrayList<>();
        result.add(Util.format("&r%s&3 sells &b%d&3x&b%s&3 for &b%s", offer.owner, offer.amount, offer.description, plugin.formatPrice(offer.price)));
        List<Object> line2 = new ArrayList<>();
        if (player.getWorld().getName().equals(offer.world)) {
            final Location loc = player.getLocation();
            int dx = offer.x - loc.getBlockX();
            int dz = offer.z - loc.getBlockZ();
            String direction = "";
            if (dz > 4) direction = "South";
            if (dz < -4) direction = "North";
            if (dx > 4) direction += "East";
            if (dx < -4) direction += "West";
            if ("".equals(direction)) direction = "Right here";
            line2.add(Util.format(" &3Location: &b%s&3 &b%d&3,&b%d&3,&b%d&3 (&b%s&3) ", offer.world, offer.x, offer.y, offer.z, direction));
        } else {
            line2.add(Util.format(" &3Location: &b%s&3 &b%d&3,&b%d&3,&b%d ", offer.world, offer.x, offer.y, offer.z));
        }
        {
            Map<String, Object> portButton = new HashMap<>();
            portButton.put("color", "dark_aqua");
            portButton.put("text", Util.format("&3[&bPort&3]"));
            Map<String, Object> clickEvent = new HashMap<>();
            portButton.put("clickEvent", clickEvent);
            clickEvent.put("action", "run_command");
            clickEvent.put("value", "/shop port " + offer.owner);
            Map<String, Object> hoverEvent = new HashMap<>();
            portButton.put("hoverEvent", hoverEvent);
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", Util.format("&3Port to %s shop", Util.genitiveName(offer.owner)));
            line2.add(portButton);
        }
        result.add(line2);
        return result;
    }

    @Override
    public void run() {
        // Header
        final int pages = (offers.size() - 1) / PAGE_LEN + 1;
        StringBuilder sb = new StringBuilder("&3&m   &3 Search results for &b");
        sb.append(items.get(0));
        for (int i = 1; i < items.size(); ++i) sb.append("&3, &b").append(items.get(i));
        if (exact) sb.append(" &3(&bexact&3)");
        Util.sendMessage(player, sb.toString());

        // First page
        plugin.messageStore.clearPages(player);
        if (offers.isEmpty()) {
            Util.sendMessage(player, "&cNo results.");
            return;
        }
        final Iterator<Offer> iter = offers.iterator();
        for (int i = 0; i < PAGE_LEN && iter.hasNext(); ++i) {
            for (Object o : formatOffer(iter.next(), player)) {
                Util.tellRaw(player, o);
            }
        }
        if (!iter.hasNext()) return;
        // Message storage
        Util.tellRaw(player, buildMoreMessage(1, pages));
        List<Object> msg = new ArrayList<>();
        int page = 1;
        while (iter.hasNext()) {
            page += 1;
            for (int i = 0; i < PAGE_LEN && iter.hasNext(); ++i) msg.addAll(formatOffer(iter.next(), player));
            if (iter.hasNext()) {
                msg.add(buildMoreMessage(page, pages));
            }
            plugin.messageStore.storePage(player, msg);
            msg.clear();
        }
    }

    private Object buildMoreMessage(int page, int pages) {
        List<Object> result = new ArrayList<>();
        result.add(Util.format("&3&m   &3 Page &b%d&3/&b%d&3 ", page, pages));
        Map<String, Object> moreButton = new HashMap<>();
        result.add(moreButton);
        moreButton.put("text", Util.format("&3[&bMore&3]"));
        Map<String, Object> clickEvent = new HashMap<>();
        moreButton.put("clickEvent", clickEvent);
        clickEvent.put("action", "run_command");
        clickEvent.put("value", "/shop more");
        Map<String, Object> hoverEvent = new HashMap<>();
        moreButton.put("hoverEvent", hoverEvent);
        hoverEvent.put("action", "show_text");
        hoverEvent.put("value", Util.format("&3View more results"));
        return result;
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
