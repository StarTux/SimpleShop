package com.winthier.simpleshop;
 
import com.winthier.simpleshop.SimpleShopPlugin;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
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
        private Block left, right;
        private ShopSign shopSign;

        public ShopChest(Inventory inventory, ShopSign shopSign, Block left, Block right) {
                this.inventory = inventory;
                this.shopSign = shopSign;
                this.left = left;
                this.right = right;
        }

        public static ShopChest getByChest(Block block) {
                if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return null;
                Chest chest = (Chest)block.getState();
                Inventory inventory = chest.getBlockInventory();
                Block left = null, right = null;
                ShopSign shopSign = null;
                if (SimpleShopPlugin.instance.allowShopSigns) {
                        inventory = inventory.getHolder().getInventory();
                        if (inventory instanceof DoubleChestInventory) {
                                DoubleChest doubleChest = ((DoubleChestInventory)inventory).getHolder();
                                left = ((Chest)doubleChest.getLeftSide()).getBlock();
                                right = ((Chest)doubleChest.getRightSide()).getBlock();
                        } else {
                                left = block;
                        }
                        shopSign = ShopSign.getByChest(left, right);
                }
                if (shopSign == null) return null;
                return new ShopChest(inventory, shopSign, left, right);
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

        public String getOwnerName() {
                return shopSign.getOwnerName();
        }

        public Player getOwner() {
                return shopSign.getOwner();
        }

        public boolean isOwner(Player player) {
                return shopSign.isOwner(player);
        }

        public boolean isAdminChest() {
                return shopSign.isAdminShop();
        }

        public boolean isBuyingChest() {
                return shopSign.isBuyingShop();
        }

        public void setPrice(double price) {
                shopSign.setPrice(price);
        }

        public void setSoldOut() {
                shopSign.setSoldOut();
        }

        public double getPrice() {
                return shopSign.getPrice();
        }

        public boolean isSellingChest() {
                return shopSign.isSellingShop();
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

        public Inventory getInventory() {
                return inventory;
        }
}
