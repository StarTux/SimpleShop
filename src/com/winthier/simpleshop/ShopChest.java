package com.winthier.simpleshop;
 
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.griefprevention.GriefPreventionHelper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

/**
 * Holder of non-unique data related to one chest representing a
 * shop.
 */
public class ShopChest {
        private Inventory inventory;
        private Block left, right;
        private ShopData shopData;

        public ShopChest(Inventory inventory, ShopData shopData, Block left, Block right) {
                this.inventory = inventory;
                this.shopData = shopData;
                this.left = left;
                this.right = right;
        }

        public static ShopChest getByChest(Block block) {
                if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return null;
                Chest chest = (Chest)block.getState();
                Inventory inventory = chest.getBlockInventory();
                Block left = null, right = null;
                ShopData shopData = null;
                inventory = inventory.getHolder().getInventory();
                if (inventory instanceof DoubleChestInventory) {
                        DoubleChest doubleChest = ((DoubleChestInventory)inventory).getHolder();
                        left = ((Chest)doubleChest.getLeftSide()).getBlock();
                        right = ((Chest)doubleChest.getRightSide()).getBlock();
                } else {
                        left = block;
                }
                shopData = ShopInventoryName.fromString(inventory.getName());
                if (shopData == null && SimpleShopPlugin.allowShopSigns()) {
                        shopData = ShopSign.getByChest(left, right);
                }
                if (shopData == null) return null;
                return new ShopChest(inventory, shopData, left, right);
        }

        public static ShopChest getBySign(Block block) {
                if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) return null;
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

        public boolean hasOwner() {
                return shopData.hasOwner();
        }

        public String getOwnerName() {
                return shopData.getOwnerName();
        }

        public Player getOwner() {
                return shopData.getOwner();
        }

        public boolean isOwner(Player player) {
                if (!shopData.hasOwner()) {
                        if (SimpleShopPlugin.useItemEconomy()) return GriefPreventionHelper.canBuild(player, left.getLocation());
                        return false;
                }
                return shopData.isOwner(player);
        }

        public boolean isAdminChest() {
                return shopData.isAdminShop();
        }

        public boolean isBuyingChest() {
                return shopData.isBuyingShop();
        }

        public boolean setPrice(double price) {
                return shopData.setPrice(price);
        }

        public boolean setSoldOut() {
                return shopData.setSoldOut();
        }

        public double getPrice() {
                return shopData.getPrice();
        }

        public boolean isSellingChest() {
                return shopData.isSellingShop();
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

        public void giveOwnerMoney(double price) {
                if (SimpleShopPlugin.useItemEconomy()) {
                        return;
                } else {
                        SimpleShopPlugin.getEconomy().depositPlayer(getOwnerName(), price);
                }
        }

        public void takeItemCurrency(Player player, double amount) {
                MaterialData mats[] = SimpleShopPlugin.getCurrencyItems().toArray(new MaterialData[0]);
                Arrays.sort(mats, new Comparator<MaterialData>() {
                                public int compare(MaterialData a, MaterialData b) {
                                        double aa = SimpleShopPlugin.getCurrencyValue(a);
                                        double bb = SimpleShopPlugin.getCurrencyValue(b);
                                        return Double.compare(aa, bb);
                                }
                        });
                for (int i = mats.length - 1; i >= 0; --i) {
                        MaterialData mat = mats[i];
                        double value = SimpleShopPlugin.getCurrencyValue(mat);
                        int needed = (int)(amount / value);
                itemLoop:
                        for (int j = inventory.getSize() - 1; j >= 0; --j) {
                                if (needed == 0) break itemLoop;
                                ItemStack item = inventory.getItem(j);
                                if (item == null || !item.getData().equals(mat)) continue itemLoop;
                                int take = Math.min(needed, item.getAmount());
                                if (take > 0) {
                                        if (item.getAmount() == take) {
                                                inventory.setItem(j, null);
                                        } else {
                                                item.setAmount(item.getAmount() - take);
                                        }
                                        amount -= value * take;
                                        needed -= take;
                                        player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(mat.getItemType(), take, (short)mat.getData()));
                                }
                        }
                }
        }

        public Inventory getInventory() {
                return inventory;
        }

        public boolean hasShopSign() {
                return shopData instanceof ShopSign;
        }

        public ShopData getShopData() {
                return shopData;
        }
}
