package com.winthier.simpleshop.listener;

import com.winthier.simpleshop.ShopChest;
import com.winthier.simpleshop.ShopData;
import com.winthier.simpleshop.ShopInventoryName;
import com.winthier.simpleshop.ShopSign;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import com.winthier.simpleshop.event.SimpleShopEvent;
import java.util.Map;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
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
     * Protect against hoppers.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (ShopChest.getByInventory(event.getSource()) != null) {
            event.setCancelled(true);
            return;
        }
        if (ShopChest.getByInventory(event.getDestination()) != null) {
            event.setCancelled(true);
            return;
        }
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
        final Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (player.isSneaking()) return;
        ShopChest shopChest = ShopChest.getByChest(event.getClickedBlock());
        if (shopChest == null && plugin.allowShopSigns()) shopChest = ShopChest.getBySign(event.getClickedBlock());
        if (shopChest == null) return;
        if ((shopChest.isOwner(player) && player.hasPermission("simpleshop.edit")) ||
            (!shopChest.isAdminChest() && player.hasPermission("simpleshop.edit.other")) ||
            (shopChest.isAdminChest() && player.hasPermission("simpleshop.edit.admin"))) {
            Double price = plugin.getPriceMap().get(player.getName());
            if (price != null) {
                if (shopChest.setPrice(price)) {
                    Util.sendMessage(player, "&bPrice of this shop chest is now %s.", plugin.formatPrice(price));
                } else {
                    Util.sendMessage(player, "&cYou can't change the price of this chest");
                }
                plugin.getPriceMap().remove(player.getName());
                event.setCancelled(true);
                return;
            }
        }
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (shopChest.isBlocked()) return;
        if (shopChest.isOwner(player)) {
            Util.sendMessage(player, "&bYour Shop Chest");
            return;
        } else {
            if (shopChest.isEmpty()) {
                shopChest.setSoldOut();
            }
            double price = shopChest.getPrice();
            String ownerName = shopChest.getOwnerName();
            if (ownerName != null && !shopChest.isAdminChest() && !SimpleShopPlugin.getEconomy().hasAccount(ownerName)) {
                Util.sendMessage(player, "&cThe owner of this shop is not available");
                event.setCancelled(true);
                return;
            }
            if (ownerName == null) {
                Util.sendMessage(player, "&bShop Chest");
            } else {
                Util.sendMessage(player, "&b%s Shop Chest", genitiveName(shopChest.getOwnerName()));
            }
            if (!Double.isNaN(price) && shopChest.isBuyingChest()) {
                Util.sendMessage(player, "&bYou can sell for %s.", plugin.formatPrice(price));
            }
            if (!Double.isNaN(price) && shopChest.isSellingChest()) {
                Util.sendMessage(player, "&bYou can buy for %s.", plugin.formatPrice(price));
            }
        }
        event.setCancelled(true);
        player.openInventory(shopChest.getInventory().getHolder().getInventory());
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
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;
        ShopData shopData = null;
        ItemStack item = event.getItemInHand();
        shopData = ShopInventoryName.fromItem(item);
        if (shopData == null) {
            ShopChest shopChest = ShopChest.getByChest(block);
            if (shopChest != null) shopData = shopChest.getShopData();
        }
        if (shopData == null) return;
        if (!player.hasPermission("simpleshop.create")) {
            Util.sendMessage(player, "&cYou don't have permission to create a shop.");
            event.setCancelled(true);
            return;
        }
        if (player.hasPermission("simpleshop.create.other") || shopData.isOwner(player)) {
            if (shopData instanceof ShopSign) {
                Util.sendMessage(player, "&bYou created a shop chest. Type &f/shop price [price]&b to change the price");
            } else {
                Util.sendMessage(player, "&bYou created a shop chest.");
            }
        } else {
            Util.sendMessage(player, "&bYou cannot create a shop chest for another player.");
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
                Util.sendMessage(player, "&cYou can't sell here.");
            }
            if (shopChest.isBuyingChest()) {
                Util.sendMessage(player, "&cUse shift click to sell items.");
            }
            return;
        }
        // shift click item into chest
        if (event.isShiftClick() && !isTopInventory && event.getCurrentItem().getType() != Material.AIR) {
            if (shopChest.isSellingChest()) {
                // deny shift clicking items in
                Util.sendMessage(player, "&cYou can't put items in.");
                return;
            }
            // try to sell item to chest
            if (shopChest.isBuyingChest()) {
                // todo implement sell chests with item economy
                double price = shopChest.getPrice();
                if (Double.isNaN(price)) {
                    Util.sendMessage(player, "&cYou can't sell here.");
                    return;
                }
                ItemStack buyItem = shopChest.getBuyItem(event.getCurrentItem());
                if (buyItem == null) {
                    Util.sendMessage(player, "&cYou can't sell this here.");
                    return;
                }
                int sold = 0;
                int restStack = event.getCurrentItem().getAmount();
            buyLoop:
                while (restStack >= buyItem.getAmount()) {
                    if (!shopChest.isAdminChest() && !plugin.getEconomy().has(shopChest.getOwnerName(), price)) {
                        Util.sendMessage(player, "&c%s has run out of money.", shopChest.getOwnerName());
                        break buyLoop;
                    }
                    if (!shopChest.isAdminChest() && !shopChest.addSlot(buyItem.clone())) {
                        Util.sendMessage(player, "&cThis chest is full.");
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
                    Util.sendMessage(player, "&bSold for %s.", plugin.formatPrice(fullPrice));
                    Player owner = shopChest.getOwner();
                    if (owner != null) {
                        Util.sendMessage(player, "&b%s sold %dx%s for %s to you.", player.getName(), soldItem.getAmount(), Util.getItemName(soldItem), plugin.formatPrice(fullPrice));
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
                    Util.sendMessage(player, "&cYou can't buy here.");
                }
                if (shopChest.isBuyingChest()) {
                    Util.sendMessage(player, "&cYou can't sell here.");
                }
            } else {
                if (shopChest.isSellingChest()) {
                    Util.sendMessage(player, "&bBuy this for %s by shift clicking.", plugin.formatPrice(price));
                } else if (shopChest.isBuyingChest()) {
                    Util.sendMessage(player, "&bWill pay %s for this item type.", plugin.formatPrice(price));
                }
            }
            return;
        }
        // shift click item out of chest
        if (event.isShiftClick() && isTopInventory && event.getCurrentItem().getType() != Material.AIR) {
            // make a purchase
            if (shopChest.isSellingChest()) {
                double currentValue = SimpleShopPlugin.getCurrencyValue(current.getData());
                double price = shopChest.getPrice();
                if (Double.isNaN(price)) {
                    Util.sendMessage(player, "&cYou can't buy here.");
                    return;
                } else if (!SimpleShopPlugin.hasMoney(player, price)) {
                    Util.sendMessage(player, "&cYou don't have enough money");
                    return;
                }
                if (!SimpleShopPlugin.takeMoney(player, shopChest.getPrice())) {
                    Util.sendMessage(player, "&cPayment error");
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
                        Util.sendMessage(player, "&cYour inventory is full.");
                        SimpleShopPlugin.giveMoney(player, price);
                        return;
                    }
                    Util.sendMessage(player, "&bBought for %s.", plugin.formatPrice(price));
                    if (!shopChest.isAdminChest()) {
                        shopChest.giveOwnerMoney(price);
                        Player owner = shopChest.getOwner();
                        if (owner != null) Util.sendMessage(owner, "&b%s bought %dx%s for %s from you.", player.getName(), item.getAmount(), Util.getItemName(item), plugin.formatPrice(price));
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
                Util.sendMessage(player, "&cYou can't buy here.");
                return;
            }
        }
    }
}
