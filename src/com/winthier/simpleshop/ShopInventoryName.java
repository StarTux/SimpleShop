package com.winthier.simpleshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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

        private static boolean startsWithIgnoreCase(String longString, String shortString) {
                if (shortString.length() > longString.length()) return false;
                if (shortString.length() == longString.length()) return longString.equalsIgnoreCase(shortString);
                String testString = longString.substring(0, shortString.length());
                return testString.equalsIgnoreCase(shortString);
        }

        public static ShopInventoryName fromString (String name) {
                String code;
                boolean selling;
                if (startsWithIgnoreCase(name, (code = SimpleShopPlugin.getShopCode()))) {
                        selling = true;
                } else if (startsWithIgnoreCase(name, (code = SimpleShopPlugin.getSellingCode()))) {
                        selling = true;
                } else if (startsWithIgnoreCase(name, (code = SimpleShopPlugin.getBuyingCode()))) {
                        selling = false;
                } else {
                        return null;
                }
                name = name.substring(code.length() + 1);
                String tokens[] = name.split(" ", 2);
                if (tokens.length != 2 && !SimpleShopPlugin.useItemEconomy()) return null;
                double price;
                try {
                        price = Double.parseDouble(tokens[0]);
                        if (price < 0.0) throw new NumberFormatException();
                } catch (NumberFormatException nfe) {
                        return null;
                }
                String owner = null;
                if (tokens.length >= 2) owner = tokens[1];
                return new ShopInventoryName(selling, price, owner);
        }

        public static ShopInventoryName fromItem(ItemStack item) {
                if (item == null) return null;
                if (item.getType() != Material.CHEST && item.getType() != Material.TRAPPED_CHEST) return null;
                if (!item.getItemMeta().hasDisplayName()) return null;
                return fromString(item.getItemMeta().getDisplayName());
        }

        @Override
        public String getOwnerName() {
                if (owner == null) return "Some Player";
                return owner;
        }

        @Override
        public Player getOwner() {
                if (owner == null) return null;
                return Bukkit.getServer().getPlayerExact(owner);
        }

        @Override
        public boolean hasOwner() {
                return owner != null;
        }

        @Override
        public boolean isOwner(Player player) {
                if (owner == null) return false;
                return player.getName().equals(owner);
        }

        @Override
        public boolean isAdminShop() {
                if (owner == null) return false;
                return SimpleShopPlugin.getAdminShopName().equals(owner);
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
        public boolean setPrice(double price) {
                return false;
        }

        @Override
        public boolean setSoldOut() {
                return false;
        }
}
