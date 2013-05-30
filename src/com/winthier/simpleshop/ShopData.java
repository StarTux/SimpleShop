package com.winthier.simpleshop;

import org.bukkit.entity.Player;

/**
 * An interface to store additional data about a chest shop.
 */
public interface ShopData {
        public String getOwnerName();
        public Player getOwner();
        public boolean isOwner(Player player);
        public boolean isAdminShop();
        public boolean isBuyingShop();
        public boolean isSellingShop();
        public double getPrice();
        public void setPrice(double price);
        public void setSoldOut();
}
