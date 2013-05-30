package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.ShopChest;
import com.winthier.simpleshop.ShopSign;
import com.winthier.simpleshop.SimpleShopPlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Listen to ShopSign related events.
 */
public class SignListener implements Listener {
        private SimpleShopPlugin plugin;

        public SignListener(SimpleShopPlugin plugin) {
                this.plugin = plugin;
        }

        public void onEnable() {
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        /**
         * We need to make sure that players can only create shops
         * with their own name on them to avoid exploits.
         * Anything more than that requires special permissions.
         */
        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
        public void onSignChange(SignChangeEvent event) {
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
                if (!ShopSign.isShopTitle(event.getLine(0))) return;
                if (!event.getPlayer().hasPermission("simpleshop.create")) {
                        event.getPlayer().sendMessage("" + ChatColor.RED + "You don't have permission to create a shop");
                        event.setCancelled(true);
                        return;
                }
                double price = 0.0;
                try {
                        price = Double.parseDouble(event.getLine(1));
                        if (price < 0.0) {
                                event.setLine(1, "10");
                        }
                } catch (NumberFormatException nfe) {
                        event.setLine(1, "10");
                }
                if (event.getLine(3).equals(SimpleShopPlugin.getAdminShopName())) {
                        if (!event.getPlayer().hasPermission("simpleshop.create.admin")) {
                                event.getPlayer().sendMessage("" + ChatColor.RED + "You don't have permission");
                                event.setCancelled(true);
                                return;
                        }
                } else if (event.getLine(3).length() > 0 && event.getPlayer().hasPermission("simpleshop.create.other")) {
                        // do nothing
                } else {
                        String name = event.getPlayer().getName();
                        if (name.length() < 16) {
                                event.setLine(2, "");
                                event.setLine(3, event.getPlayer().getName());
                        } else {
                                event.setLine(2, name.substring(15));
                                event.setLine(3, name.substring(0, 16));
                        }
                }
                if (event.getBlock().getRelative(0, -1, 0).getType() != Material.CHEST) {
                        event.getPlayer().sendMessage("" + ChatColor.GREEN + "You created a shop sign. Put a chest underneath to sell things.");
                } else {
                        event.getPlayer().sendMessage("" + ChatColor.GREEN + "You created a shop chest. Type " + ChatColor.WHITE + "/shop price [price]" + ChatColor.GREEN + " to change the price");
                }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlace(BlockPlaceEvent event) {
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
                if (event.getBlockPlaced().getType() == Material.CHEST) {
                        ShopChest shopChest = ShopChest.getByChest(event.getBlockPlaced());
                        if (shopChest != null && event.getPlayer().equals(shopChest.getOwner())) {
                                event.getPlayer().sendMessage("" + ChatColor.GREEN + "You created a shop chest. Type " + ChatColor.WHITE + "/shop price [price]" + ChatColor.GREEN + " to change the price");
                        }
                }
        }
}
