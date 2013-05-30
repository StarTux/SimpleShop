package com.winthier.simpleshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * ShopData stored in the name of a chest
 */
public class ShopInventoryName implements ShopData {
        private final boolean selling;
        private final double price;
        private final String owner;

        public ShopInventoryName(boolean selling, double price, String owner) {
                this.selling = selling;
                this.price = price;
                this.owner = owner;
        }

        public static ShopInventoryName fromString (String name) {
                String code;
                boolean selling;
                if (name.startsWith((code = SimpleShopPlugin.getShopCode()))) {
                        selling = true;
                } else if (name.startsWith((code = SimpleShopPlugin.getSellingCode()))) {
                        selling = true;
                } else if (name.startsWith((code = SimpleShopPlugin.getBuyingCode()))) {
                        selling = false;
                } else {
                        return null;
                }
                name = name.substring(code.length() + 1);
                String tokens[] = name.split(" ", 2);
                if (tokens.length != 2) return null;
                double price;
                try {
                        price = Double.parseDouble(tokens[0]);
                        if (price < 0.0) throw new NumberFormatException();
                } catch (NumberFormatException nfe) {
                        return null;
                }
                return new ShopInventoryName(selling, price, tokens[1]);
        }

        @Override
        public String getOwnerName() {
                return owner;
        }

        @Override
        public Player getOwner() {
                return Bukkit.getServer().getPlayerExact(getOwnerName());
        }

        @Override
        public boolean isOwner(Player player) {
                return getOwnerName().equals(player.getName());
        }

        @Override
        public boolean isAdminShop() {
                return getOwnerName().equals(SimpleShopPlugin.getAdminShopName());
        }

        @Override
        public boolean isBuyingShop() {
                return !selling;
        }

        @Override
        public boolean isSellingShop() {
                return selling;
        }

        @Override
        public double getPrice() {
                return price;
        }

        @Override
        public void setPrice(double price) {
        }

        @Override
        public void setSoldOut() {
        }
}
