package com.winthier.simpleshop.sql;

import com.winthier.libsql.ConnectionManager;
import com.winthier.simpleshop.ShopType;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.event.SimpleShopEvent;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class SQLManager implements Listener {
    private SimpleShopPlugin plugin;
    private ConnectionManager connectionManager;

    public SQLManager(SimpleShopPlugin plugin) {
        connectionManager = new ConnectionManager(plugin, plugin.getConfig().getConfigurationSection("sql"));
        this.plugin = plugin;
    }

    public void onEnable() {
        connectionManager.start();
        connectionManager.queueRequest(new CreateTableRequest());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void onDisable() {
        connectionManager.stop();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSimpleShop(SimpleShopEvent event) {
        LogTransactionRequest request = new LogTransactionRequest(event);
        connectionManager.queueRequest(request);
    }

    public void listTransactions(CommandSender sender, String name, int page) {
        ListTransactionsRequest request = new ListTransactionsRequest(plugin, sender, name, page);
        connectionManager.queueRequest(request);
    }

    public void sendAveragePrice(CommandSender sender, ItemStack item, int days) {
        AveragePriceRequest request = new AveragePriceRequest(plugin, sender, item, days);
        connectionManager.queueRequest(request);
    }

    public void updateTable() {
        connectionManager.queueRequest(new UpdateTableRequest());
    }

    public void sendShopPlayerStatistics(CommandSender sender, String owner, int days, int page) {
        connectionManager.queueRequest(new ShopStatisticsRequest(plugin, sender, owner, ShopStatisticsRequest.Type.PLAYER, days, page));
    }

    public void sendShopItemStatistics(CommandSender sender, String owner, int days, int page) {
        connectionManager.queueRequest(new ShopStatisticsRequest(plugin, sender, owner, ShopStatisticsRequest.Type.ITEM, days, page));
    }

    public void logOffer(ShopType shopType, String owner, Location location, int amount, double price, String description) {
        connectionManager.queueRequest(new LogOfferRequest(shopType, owner, location, amount, price, description));
    }

    public void updateVersion(String name) {
        connectionManager.queueRequest(new UpdateVersionRequest(name));
    }

    public void searchOffers(Player player, ShopType shopType, List<String> items, boolean exact) {
        connectionManager.queueRequest(new SearchOffersRequest(plugin, player, shopType, items, exact));
    }

    public void portShop(Player player, String ownerName) {
        connectionManager.queueRequest(new ShopPortRequest(plugin, player, ownerName));
    }

    public void clearOffers() {
        connectionManager.queueRequest(new ClearOffersRequest());
    }
}
