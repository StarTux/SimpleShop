package com.winthier.simpleshop.event;

import com.winthier.simpleshop.ShopChest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class SimpleShopEvent extends Event {
        private static HandlerList handlers = new HandlerList();
        private Player player;
        private ShopChest shopChest;
        private ItemStack item;
        private double price;

        public SimpleShopEvent(Player player, ShopChest shopChest, ItemStack item, double price) {
                this.player = player;
                this.shopChest = shopChest;
                this.item = item.clone();
                this.price = price;
        }

        public static HandlerList getHandlerList() {
                return handlers;
        }

        @Override
        public HandlerList getHandlers() {
                return handlers;
        }

        public Player getPlayer() {
                return player;
        }

        public ShopChest getShopChest() {
                return shopChest;
        }

        public ItemStack getItem() {
                return item;
        }

        public double getPrice() {
                return price;
        }
}
