package com.winthier.simpleshop.sql;

import com.winthier.libsql.ConnectionManager;
import com.winthier.libsql.PluginSQLRequest;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.event.SimpleShopEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
                connectionManager.queueRequest(new CreateTableRequest(plugin));
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        public void onDisable() {
                connectionManager.stop();
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onSimpleShop(SimpleShopEvent event) {
                LogTransactionRequest request = new LogTransactionRequest(plugin, event);
                connectionManager.queueRequest(request);
        }

        public void importLogs() {
                LogImporter task = new LogImporter(plugin, connectionManager);
                task.start();
        }

        public void listTransactions(CommandSender sender, String name, int page) {
                ListTransactionsRequest request = new ListTransactionsRequest(plugin, sender, name, page);
                connectionManager.queueRequest(request);
        }

        public void sendAveragePrice(CommandSender sender, ItemStack item) {
                AveragePriceRequest request = new AveragePriceRequest(plugin, sender, item);
                connectionManager.queueRequest(request);
        }
}
