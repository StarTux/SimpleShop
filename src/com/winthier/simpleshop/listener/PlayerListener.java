package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.ShopChest;
import com.winthier.simpleshop.ShopData;
import com.winthier.simpleshop.ShopInventoryName;
import com.winthier.simpleshop.ShopSign;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.event.SimpleShopEvent;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
        private SimpleShopPlugin plugin;

        public PlayerListener(SimpleShopPlugin plugin) {
                this.plugin = plugin;
        }

        public void onEnable() {
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        /**
         * When a right clicked chest or sign is identified as
         * part of a shop chest, we want to send an informative
         * message and open the chest. The opening is done
         * manually and the event cancelled so protection plugins
         * don't get involved later.
         */
        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerInteract(PlayerInteractEvent event) {
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
                if (event.getClickedBlock() == null) return;
                if (event.getPlayer().isSneaking() && event.getPlayer().getItemInHand() != null) return;
                ShopChest shopChest = ShopChest.getByChest(event.getClickedBlock());
                if (shopChest == null && plugin.allowShopSigns()) shopChest = ShopChest.getBySign(event.getClickedBlock());
                if (shopChest == null) return;
                if ((shopChest.isOwner(event.getPlayer()) && event.getPlayer().hasPermission("simpleshop.edit")) ||
                    (!shopChest.isAdminChest() && event.getPlayer().hasPermission("simpleshop.edit.other")) ||
                    (shopChest.isAdminChest() && event.getPlayer().hasPermission("simpleshop.edit.admin"))) {
                        Double price = plugin.getPriceMap().get(event.getPlayer().getName());
                        if (price != null) {
                                if (shopChest.setPrice(price)) {
                                        event.getPlayer().sendMessage("" + ChatColor.GREEN + "Price of this shop chest is now " + plugin.formatPrice(price) + ".");
                                } else {
                                        event.getPlayer().sendMessage("" + ChatColor.RED + "You can't change the price of this chest");
                                }
                                plugin.getPriceMap().remove(event.getPlayer().getName());
                                event.setCancelled(true);
                                return;
                        }
                }
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
                if (shopChest.isBlocked()) return;
                if (shopChest.isOwner(event.getPlayer())) {
                        event.getPlayer().sendMessage("" + ChatColor.GREEN + "Your Shop Chest");
                } else {
                        if (shopChest.isEmpty()) {
                                shopChest.setSoldOut();
                        }
                        double price = shopChest.getPrice();
                        String ownerName = shopChest.getOwnerName();
                        if (ownerName == null) {
                                event.getPlayer().sendMessage("" + ChatColor.GREEN + "Shop Chest");
                        } else {
                                event.getPlayer().sendMessage("" + ChatColor.GREEN + genitiveName(shopChest.getOwnerName()) + " Shop Chest");
                        }
                        if (!Double.isNaN(price) && shopChest.isBuyingChest()) {
                                event.getPlayer().sendMessage("" + ChatColor.GREEN + "You can sell for " + plugin.formatPrice(price) + ".");
                        }
                        if (!Double.isNaN(price) && shopChest.isSellingChest()) {
                                event.getPlayer().sendMessage("" + ChatColor.GREEN + "You can buy for " + plugin.formatPrice(price) + ".");
                        }
                }
                event.setCancelled(true);
                event.getPlayer().openInventory(shopChest.getInventory().getHolder().getInventory());
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onInventoryClose(InventoryCloseEvent event) {
                if (SimpleShopPlugin.useItemEconomy()) {
                        if (event.getPlayer() instanceof Player) {
                                Player player = (Player)event.getPlayer();
                                if (player.getGameMode() == GameMode.CREATIVE) return;
                                ShopChest shop = ShopChest.getByInventory(event.getInventory());
                                if (shop != null) {
                                        double amount = SimpleShopPlugin.getPaidItems(player);
                                        shop.takeItemCurrency(player, amount);
                                }
                                SimpleShopPlugin.resetPaidItems(player);
                        }
                }
        }

        /**
         * Helper function to create the genitive case of a player
         * obeying apostrophy rules. Examples:
         * Sirabell => Sirabell's
         * StarTux => StarTux'
         */
        private String genitiveName(String name) {
                if (name.endsWith("s") || name.endsWith("x") || name.endsWith("z")) {
                        return name + "'";
                }
                return name + "'s";
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlace(BlockPlaceEvent event) {
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
                Block block = event.getBlock();
                if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;
                ShopData shopData = null;
                ItemStack item = event.getPlayer().getItemInHand();
                shopData = ShopInventoryName.fromItem(event.getPlayer().getItemInHand());
                if (shopData == null) {
                        ShopChest shopChest = ShopChest.getByChest(block);
                        if (shopChest != null) shopData = shopChest.getShopData();
                }
                if (shopData == null) return;
                if (!event.getPlayer().hasPermission("simpleshop.create")) {
                        event.getPlayer().sendMessage("" + ChatColor.RED + "You don't have permission to create a shop.");
                        event.setCancelled(true);
                        return;
                }
                if (event.getPlayer().hasPermission("simpleshop.create.other") || shopData.isOwner(event.getPlayer())) {
                        if (shopData instanceof ShopSign) {
                                event.getPlayer().sendMessage("" + ChatColor.GREEN + "You created a shop chest. Type " + ChatColor.WHITE + "/shop price [price]" + ChatColor.GREEN + " to change the price");
                        } else {
                                event.getPlayer().sendMessage("" + ChatColor.GREEN + "You created a shop chest.");
                        }
                } else if (!shopData.hasOwner() && plugin.useItemEconomy()) {
                        // do nothing
                } else {
                        event.getPlayer().sendMessage("" + ChatColor.RED + "You cannot create a shop chest for another player.");
                        event.setCancelled(true);
                        return;
                }
        }

        /**
         * Allow dragging if it's only in the bottom inventory.
         * Ignore if player is owner or in creative.
         */
        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onInventoryDrag(InventoryDragEvent event) {
                ShopChest shopChest = ShopChest.getByInventory(event.getInventory());
                if (shopChest == null) return;
                if (!(event.getWhoClicked() instanceof Player)) return;
                Player player = (Player)event.getWhoClicked();
                if (player.getGameMode() == GameMode.CREATIVE) return;
                if (shopChest.isOwner(player)) return;
                boolean isTopInventory = false;
                for (Integer slot : event.getRawSlots()) {
                        if (slot < event.getView().getTopInventory().getSize()) {
                                isTopInventory = true;
                                break;
                        }
                }
                if (!isTopInventory) return;
		event.setCancelled(true);
                event.getView().setCursor(event.getOldCursor());
        }

        /**
         * Shop activity is decided by click events in
         * inventories. As a general rule of thumb, when an event
         * causes a purchase, we do the work manually instead of
         * relying on vanilla logic.
         */
        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onInventoryClick(InventoryClickEvent event) {
                if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) return;
                if (event.getRawSlot() < 0) return;
                if (!(event.getWhoClicked() instanceof Player)) return;
                Player player = (Player)event.getWhoClicked();
                if (player.getGameMode() == GameMode.CREATIVE) return;
                ShopChest shopChest = ShopChest.getByInventory(event.getInventory());
                if (shopChest == null) return;
                boolean isTopInventory = (event.getRawSlot() < event.getView().getTopInventory().getSize());
                boolean isOwner = shopChest.isOwner(player);
                if (isOwner) return;
		// cancel everything
		ItemStack cursor = event.getCursor().clone();
		ItemStack current = event.getCurrentItem().clone();
                // allow left or right clicking in your own inventory
                if (!event.isShiftClick() && !isTopInventory) {
                        switch (event.getClick()) {
                        case LEFT:
                        case RIGHT:
                                return;
                        }
                }
		event.setCancelled(true);
                // deny clicking items in for non-owners
                if (!event.isShiftClick() && isTopInventory && event.getCursor().getType() != Material.AIR) {
                        if (shopChest.isSellingChest()) {
                                player.sendMessage("" + ChatColor.RED + "You can't sell here.");
                        }
                        if (shopChest.isBuyingChest()) {
                                player.sendMessage("" + ChatColor.RED + "Use shift click to sell items.");
                        }
                        return;
                }
                // shift click item into chest
                if (event.isShiftClick() && !isTopInventory && event.getCurrentItem().getType() != Material.AIR) {
                        if (shopChest.isSellingChest()) {
                                // payment
                                if (SimpleShopPlugin.useItemEconomy()) {
                                        double value = plugin.getCurrencyValue(current.getData());
                                        if (value > 0.0) {
                                                Map<Integer, ItemStack> retours = event.getInventory().addItem(current.clone());
                                                ItemStack retour = retours.get(0);
                                                if (retour == null) {
                                                        event.setCurrentItem(null);
                                                } else {
                                                        event.setCurrentItem(retour.clone());
                                                }
                                                int amount = current.getAmount();
                                                if (retour != null) amount -= retour.getAmount();
                                                plugin.addPaidItems(player, value * amount);
                                                return;
                                        }
                                }
                                // deny shift clicking items in
                                player.sendMessage("" + ChatColor.RED + "You can't put items in.");
                                return;
                        }
                        // try to sell item to chest
                        if (shopChest.isBuyingChest()) {
                                // todo implement sell chests with item economy
                                if (SimpleShopPlugin.useItemEconomy()) {
                                        return;
                                }
                                double price = shopChest.getPrice();
                                if (Double.isNaN(price)) {
                                        player.sendMessage("" + ChatColor.RED + "You can't sell here.");
                                        return;
                                }
                                ItemStack buyItem = shopChest.getBuyItem(event.getCurrentItem());
                                if (buyItem == null) {
                                        player.sendMessage("" + ChatColor.RED + "You can't sell this here.");
                                        return;
                                }
                                int sold = 0;
                                int restStack = event.getCurrentItem().getAmount();
                        buyLoop:
                                while (restStack >= buyItem.getAmount()) {
                                        if (!shopChest.isAdminChest() && !plugin.getEconomy().has(shopChest.getOwnerName(), price)) {
                                                player.sendMessage("" + ChatColor.RED + shopChest.getOwnerName() + " has run out of money.");
                                                break buyLoop;
                                        }
                                        if (!shopChest.isAdminChest() && !shopChest.addSlot(buyItem.clone())) {
                                                player.sendMessage("" + ChatColor.RED + "This chest is full.");
                                                shopChest.setSoldOut();
                                                break buyLoop;
                                        }
                                        sold += 1;
                                        restStack -= buyItem.getAmount();
                                        if (!shopChest.isAdminChest()) {
                                                plugin.getEconomy().withdrawPlayer(shopChest.getOwnerName(), price);
                                        }
                                }
                                if (sold > 0) {
                                        double fullPrice = price * (double)sold;
                                        ItemStack soldItem = buyItem.clone();
                                        soldItem.setAmount(sold * buyItem.getAmount());
                                        plugin.getEconomy().depositPlayer(player.getName(), fullPrice);
                                        if (restStack == 0) {
                                                event.setCurrentItem(null);
                                        } else {
                                                event.getCurrentItem().setAmount(restStack);
                                        }
                                        player.sendMessage("" + ChatColor.GREEN + "Sold for " + plugin.formatPrice(fullPrice) + ".");
                                        Player owner = shopChest.getOwner();
                                        if (owner != null) {
                                                owner.sendMessage("" + ChatColor.GREEN + player.getName() + " sold " + soldItem.getAmount() + "x" + plugin.getItemName(soldItem) + " for " + plugin.formatPrice(fullPrice) + " to you.");
                                        }
                                        plugin.logSale(shopChest, player.getName(), soldItem, fullPrice);
                                        plugin.getServer().getPluginManager().callEvent(new SimpleShopEvent(player, shopChest, soldItem, fullPrice));
                                }
                                return;
                        }
                }
                // single click chest slot to try to take item out
                if (!event.isShiftClick() && isTopInventory && event.getCurrentItem().getType() != Material.AIR) {
                        // deny taking items via single click for non-owners to avoid accidents
                        double price = shopChest.getPrice();
                        if (Double.isNaN(price)) {
                                if (shopChest.isSellingChest()) {
                                        player.sendMessage("" + ChatColor.RED + "You can't buy here.");
                                }
                                if (shopChest.isBuyingChest()) {
                                        player.sendMessage("" + ChatColor.RED + "You can't sell here.");
                                }
                        } else {
                                if (SimpleShopPlugin.useItemEconomy() && SimpleShopPlugin.getCurrencyValue(current.getData()) > 0.0) {
                                        player.sendMessage("" + ChatColor.RED + "Don't take money!");
                                } else if (shopChest.isSellingChest()) {
                                        player.sendMessage("" + ChatColor.GREEN + "Buy this for " + plugin.formatPrice(price) + " by shift clicking.");
                                } else if (shopChest.isBuyingChest()) {
                                        player.sendMessage("" + ChatColor.GREEN + "Will pay " + plugin.formatPrice(price) + " for this item type.");
                                }
                        }
                        return;
                }
                // shift click item out of chest
                if (event.isShiftClick() && isTopInventory && event.getCurrentItem().getType() != Material.AIR) {
                        // make a purchase
                        if (shopChest.isSellingChest()) {
                                double currentValue = SimpleShopPlugin.getCurrencyValue(current.getData());
                                if (SimpleShopPlugin.useItemEconomy() && currentValue > 0.0) {
                                        int canTake = (int)(SimpleShopPlugin.getPaidItems(player) / currentValue);
                                        int willTake = Math.min(current.getAmount(), canTake);
                                        if (willTake == 0) {
                                                player.sendMessage("" + ChatColor.RED + "Don't take money!");
                                                return;
                                        }
                                        int newAmount = current.getAmount() - willTake;
                                        if (newAmount <= 0) {
                                                event.setCurrentItem(null);
                                        } else {
                                                event.getCurrentItem().setAmount(newAmount);
                                        }
                                        ItemStack retour = current.clone();
                                        retour.setAmount(willTake);
                                        SimpleShopPlugin.addPaidItems(player, (double)willTake * -currentValue);
                                        Map<Integer, ItemStack> retours = player.getInventory().addItem(retour);
                                        if (!retours.isEmpty()) player.getWorld().dropItem(player.getEyeLocation(), retours.get(0));
                                        return;
                                }
                                double price = shopChest.getPrice();
                                if (Double.isNaN(price)) {
                                        player.sendMessage("" + ChatColor.RED + "You can't buy here.");
                                        return;
                                } else if (!SimpleShopPlugin.hasMoney(player, price)) {
                                        player.sendMessage("" + ChatColor.RED + "You don't have enough money");
                                        return;
                                }
                                if (!SimpleShopPlugin.takeMoney(player, shopChest.getPrice())) {
                                        player.sendMessage("" + ChatColor.RED + "Payment error");
                                        return;
                                } else {
                                        // purchase made
                                        ItemStack item = event.getCurrentItem();
                                        Map<Integer, ItemStack> retours;
                                        retours = player.getInventory().addItem(item.clone());
                                        if (!retours.isEmpty()) {
                                                ItemStack retour = item.clone();
                                                for (ItemStack is : retours.values()) retour.setAmount(retour.getAmount() - is.getAmount());
                                                if (retour.getAmount() > 0) player.getInventory().removeItem(retour);
                                                player.sendMessage("" + ChatColor.RED + "Your inventory is full.");
                                                SimpleShopPlugin.giveMoney(player, price);
                                                return;
                                        }
                                        player.sendMessage("" + ChatColor.GREEN + "Bought for " + plugin.formatPrice(price) + ".");
                                        if (!shopChest.isAdminChest()) {
                                                shopChest.giveOwnerMoney(price);
                                                Player owner = shopChest.getOwner();
                                                if (owner != null) owner.sendMessage("" + ChatColor.GREEN + player.getName() + " bought " + item.getAmount() + "x" + plugin.getItemName(item) + " for " + plugin.formatPrice(price) + " from you.");
                                                event.setCurrentItem(null);
                                        } else {
                                                event.setCurrentItem(event.getCurrentItem());
                                        }
                                        plugin.logSale(shopChest, player.getName(), item, price);
                                        plugin.getServer().getPluginManager().callEvent(new SimpleShopEvent(player, shopChest, item, price));
                                        if (shopChest.isEmpty()) {
                                                shopChest.setSoldOut();
                                        }
                                        return;
                                }
                        }
                        if (shopChest.isBuyingChest()) {
                                if (SimpleShopPlugin.useItemEconomy() && SimpleShopPlugin.getCurrencyValue(current.getData()) > 0.0) {
                                        player.sendMessage("" + ChatColor.RED + "Don't take money!");
                                        return;
                                }
                                player.sendMessage("" + ChatColor.RED + "You can't buy here.");
                                return;
                        }
                }
        }
}
