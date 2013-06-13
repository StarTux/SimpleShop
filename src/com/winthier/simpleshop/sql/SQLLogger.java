package com.winthier.simpleshop.sql;

import com.winthier.libsql.ConnectionManager;
import com.winthier.libsql.PluginSQLRequest;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.event.SimpleShopEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SQLLogger implements Listener {
        private SimpleShopPlugin plugin;
        public ConnectionManager connectionManager;

        public SQLLogger(SimpleShopPlugin plugin, ConfigurationSection section) {
                connectionManager = new ConnectionManager(plugin, section);
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
}
