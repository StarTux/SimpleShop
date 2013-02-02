package com.winthier.simpleshop;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Holder of non-unique data related to one chest representing a
 * shop.
 */
public class ShopChest {
        private Inventory inventory;
        private Block sign;
        private Block left, right;

        public ShopChest(Inventory inventory, Block sign, Block left, Block right) {
                this.inventory = inventory;
                this.sign = sign;
                this.left = left;
                this.right = right;
        }

        public static ShopChest getByChest(Block block) {
                if (block.getType() != Material.CHEST) return null;
                Chest chest = (Chest)block.getState();
                Inventory inventory = chest.getBlockInventory();
                Sign sign = null;
                Block left = null, right = null;
                inventory = inventory.getHolder().getInventory();
                if (inventory instanceof DoubleChestInventory) {
                        DoubleChest doubleChest = ((DoubleChestInventory)inventory).getHolder();
                        left = ((Chest)doubleChest.getLeftSide()).getBlock();
                        right = ((Chest)doubleChest.getRightSide()).getBlock();
                        if (isSign(left.getRelative(0, 1, 0).getType())) {
                                sign = (Sign)left.getRelative(0, 1, 0).getState();
                                if (!isShopTitle(sign.getLine(0))) sign = null;
                        }
                        if (sign == null && isSign(right.getRelative(0, 1, 0).getType())) {
                                sign = (Sign)right.getRelative(0, 1, 0).getState();
                        }
                } else {
                        left = block;
                        if (isSign(block.getRelative(0, 1, 0).getType())) {
                                sign = (Sign)block.getRelative(0, 1, 0).getState();
                        }
                }
                if (sign == null) return null;
                if (!isShopTitle(sign.getLine(0))) return null;
                return new ShopChest(inventory, sign.getBlock(), left, right);
        }

        public static ShopChest getBySign(Block block) {
                return getByChest(block.getRelative(0, -1, 0));
        }

        public static ShopChest getByInventory(Inventory inventory) {
                if (inventory.getHolder() instanceof Chest) {
                        return getByChest(((Chest)inventory.getHolder()).getBlock());
                }
                if (inventory instanceof DoubleChestInventory) {
                        return getByChest(((DoubleChestInventory)inventory).getHolder().getLocation().getBlock());
                }
                return null;
        }

        public List<Block> getBlocks() {
                if (right == null) return Arrays.asList(left);
                return Arrays.asList(left, right);
        }

        public Location getLocation() {
                return left.getLocation();
        }

        private static boolean blocksChest(Block block) {
                Material mat = block.getType();
                return mat.isOccluding();
        }

        public boolean isBlocked() {
                if (blocksChest(left.getRelative(0, 1, 0))) return true;
                if (right != null && blocksChest(right.getRelative(0, 1, 0))) return true;
                return false;
        }

        private Sign getSign() {
                return (Sign)sign.getState();
        }
        
        public String getOwnerName() {
                if (isAdminChest()) return "The Bank";
                return getSign().getLine(3) + getSign().getLine(2);
        }

        public Player getOwner() {
                if (isAdminChest()) return null;
                return Bukkit.getServer().getPlayerExact(getOwnerName());
        }

        public boolean isOwner(Player player) {
                if (isAdminChest()) return false;
                return player.getName().equals(getOwnerName());
        }

        public boolean isAdminChest() {
                return getSign().getLine(3).equals(getAdminChestName());
        }

        public double getPrice() {
                try {
                        return Double.parseDouble(getSign().getLine(1));
                } catch (NumberFormatException nfe) {
                        return Double.NaN;
                }
        }

        public void setPrice(double price) {
                if (price < 0.0) price = 0.0;
                Sign state = getSign();
                String tag = "" + price;
                String tags[] = tag.split("\\.");
                if (tags.length == 2 && tags[1].equals("0")) {
                        state.setLine(1, "" + tags[0]);
                } else {
                        state.setLine(1, String.format("%.02f", price));
                }
                state.update();
        }

        public boolean isEmpty() {
                for (ItemStack item : getInventory().getContents()) {
                        if (item != null) return false;
                }
                return true;
        }

        public boolean addSlot(ItemStack item) {
                Inventory inventory = getInventory();
                for (int i = 0; i < inventory.getSize(); ++i) {
                        if (inventory.getItem(i) == null) {
                                inventory.setItem(i, item);
                                return true;
                        }
                }
                return false;
        }

        public ItemStack getBuyItem(ItemStack offer) {
                for (ItemStack item : getInventory()) {
                        if (item != null &&
                            item.getTypeId() == offer.getTypeId() &&
                            item.getDurability() == offer.getDurability() &&
                            item.getItemMeta().equals(offer.getItemMeta())) {
                                return item.clone();
                        }
                }
                return null;
        }

        public void setSoldOut() {
                Sign state = getSign();
                state.setLine(1, "SOLD OUT");
                state.update();
        }

        public Inventory getInventory() {
                return inventory;
        }

        public static boolean isSign(Material mat) {
                return mat == Material.SIGN_POST || mat == Material.WALL_SIGN;
        }

        public static String getAdminChestName() {
                return "The Bank";
        }

        public static String getShopSignTitle() {
                return "[shop]";
        }

        public static String getSellingSignTitle() {
                return "[buy]";
        }

        public static String getBuyingSignTitle() {
                return "[sell]";
        }

        public boolean isBuyingChest() {
                return getSign().getLine(0).equalsIgnoreCase(getBuyingSignTitle());
        }

        public boolean isSellingChest() {
                return getSign().getLine(0).equalsIgnoreCase(getSellingSignTitle()) ||
                        getSign().getLine(0).equalsIgnoreCase(getShopSignTitle());
        }

        public static boolean isShopTitle(String line) {
                return (line.equalsIgnoreCase(getSellingSignTitle()) ||
                        line.equalsIgnoreCase(getBuyingSignTitle()) ||
                        line.equalsIgnoreCase(getShopSignTitle()));
        }
}
