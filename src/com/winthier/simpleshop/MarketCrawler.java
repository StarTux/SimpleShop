package com.winthier.simpleshop;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.List;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class MarketCrawler {
    private final SimpleShopPlugin plugin;

    // Where's the market?
    private List<String> regions = null;
    // Timing
    private int chunksPerTick = 1;
    private int interval = 60;
    // State
    private int regionIndex = 0;
    private boolean regionSelected = false;
    private World world = null;
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private int chunkMinX, chunkMaxX;
    private int chunkMinZ, chunkMaxZ;
    private int chunkX, chunkZ;
    // Task
    private BukkitRunnable task = null;

    public MarketCrawler(SimpleShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(ConfigurationSection config) {
        regions = config.getStringList("Regions");
        chunksPerTick = config.getInt("ChunksPerTick", 1);
        interval = config.getInt("Interval");
    }

    private void startTask(long delay) {
        stopTask();
        task = new BukkitRunnable() {
            public void run() {
                iter();
            }
        };
        task.runTaskTimer(plugin, delay, 1L);
    }

    private void stopTask() {
        if (task == null) return;
        try {
            task.cancel();
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        } finally {
            task = null;
        }
    }

    public void onEnable() {
        regionSelected = false;
        regionIndex = 0;
        if (regions.size() > 0) {
            startTask(0L);
        }
    }

    public void delay() {
        startTask((long)interval * 20L * 60L);
    }

    public void onDisable() {
        stopTask();
    }

    public void iter() {
        if (!regionSelected) {
            try {
                if (!nextRegion()) {
                    plugin.sqlManager.flushOfferCache();
                    plugin.sqlManager.updateVersion("offers");
                    plugin.sqlManager.clearOffers();
                    delay();
                    plugin.getLogger().info("Market crawler finished. Waiting " + interval + " minutes.");
                } else if (regionIndex == 1) {
                    plugin.getLogger().info("Market crawler started.");
                }
            } catch (InvalidConfigurationException ice) {
                plugin.getLogger().warning(ice.getMessage());
                onDisable();
            }
            return;
        }
        for (int i = 0; i < chunksPerTick; ++i) {
            checkChunk();
            if (!nextChunk()) return;
        }
    }

    private boolean nextRegion() throws InvalidConfigurationException {
        regionSelected = false;

        if (regionIndex >= regions.size()) {
            regionIndex = 0;
            return false;
        }
        String item = regions.get(regionIndex++);

        String[] tokens = item.split(":", 2);
        if (tokens.length != 2) {
            throw new InvalidConfigurationException("Expected \"region:world\", found: \"" + item + "\".");
        }
        String worldName = tokens[0];
        String regionName = tokens[1];
        world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            throw new InvalidConfigurationException("World not found: " + worldName + ".");
        }

        RegionManager rm = WGBukkit.getRegionManager(world);
        if (rm == null) {
            throw new InvalidConfigurationException("No RegionManager found for world" + world.getName() + ".");
        }

        ProtectedRegion region = rm.getRegion(regionName);
        if (region == null) {
            throw new InvalidConfigurationException("Region " + regionName + " does not exist in world " + world.getName() + ".");
        }

        BlockVector vec;
        vec = region.getMinimumPoint();
        minX = vec.getBlockX();
        minY = vec.getBlockY();
        minZ = vec.getBlockZ();

        vec = region.getMaximumPoint();
        maxX = vec.getBlockX();
        maxY = vec.getBlockY();
        maxZ = vec.getBlockZ();

        Chunk minChunk = world.getBlockAt(minX, minY, minZ).getChunk();
        Chunk maxChunk = world.getBlockAt(maxX, maxY, maxZ).getChunk();

        chunkMinX = minChunk.getX();
        chunkMinZ = minChunk.getZ();

        chunkMaxX = maxChunk.getX();
        chunkMaxZ = maxChunk.getZ();

        chunkX = chunkMinX;
        chunkZ = chunkMinZ;

        regionSelected = true;
        return true;
    }

    /**
     * @return false if there are not more chunks, true otherwise.
     */
    private boolean nextChunk() {
        if (++chunkX > chunkMaxX) {
            chunkX = chunkMinX;
            if (++chunkZ > chunkMaxZ) {
                chunkZ = chunkMinZ;
                regionSelected = false;
                return false;
            }
        }
        return true;
    }

    private void checkChunk() {
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        chunk.load();
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Chest) {
                Inventory inv = ((Chest)state).getInventory();
                ShopChest shop = ShopChest.getByInventory(inv);
                if (shop != null) checkShopChest(shop);
            }
        }
    }

    private void checkShopChest(ShopChest shop) {
        final ShopType shopType = shop.getShopType();
        final double price = shop.getPrice();
        if (Double.isNaN(price)) return;
        final String owner = shop.getOwnerName();
        if (owner == null) return;
        final Inventory inv = shop.getInventory();
        for (ItemStack item : inv) {
            if (item == null || item.getType() == Material.AIR) continue;
            logItem(shopType, owner, shop.getLocation(), price, item);
        }
    }

    public static String getEnchantmentName(Enchantment enchantment) {
        switch (enchantment.getId()) {
        case 0: return "Protection";
        case 1: return "Fire Protection";
        case 2: return "Feather Falling";
        case 3: return "Blast Protection";
        case 4: return "Projectile Protection";
        case 5: return "Respiration";
        case 6: return "Aqua Affinity";
        case 7: return "Thorns";
        case 16: return "Sharpness";
        case 17: return "Smite";
        case 18: return "Bane of Arthropods";
        case 19: return "Knockback";
        case 20: return "Fire Aspect";
        case 21: return "Looting";
        case 48: return "Power";
        case 49: return "Punch";
        case 50: return "Flame";
        case 51: return "Infinity";
        case 32: return "Efficiency";
        case 33: return "Silk Touch";
        case 34: return "Unbreaking";
        case 35: return "Fortune";
        case 61: return "Luck of the Sea";
        case 62: return "Lure";
        default: return Util.niceEnumName(enchantment.getName());
        }
    }

    public static String getItemDescription(ItemStack item) {
        StringBuilder desc = new StringBuilder(Util.getItemName(item));
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            Map<Enchantment, Integer> enchants;
            enchants = meta.getEnchants();
            if (enchants.isEmpty() && meta instanceof EnchantmentStorageMeta) {
                enchants = ((EnchantmentStorageMeta)meta).getStoredEnchants();
            }
            if (enchants != null && !enchants.isEmpty()) {
                for (Enchantment enchant : enchants.keySet()) {
                    desc.append(", ");
                    desc.append(getEnchantmentName(enchant));
                    desc.append(" ");
                    desc.append(enchants.get(enchant));
                }
            }
            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta)meta;
                if (skull.hasOwner()) {
                    desc.append(" <");
                    desc.append(skull.getOwner());
                    desc.append(">");
                }
            }
        }
        return desc.toString();
    }

    private void logItem(ShopType shopType, String owner, Location location, double price, ItemStack item) {
        String desc = getItemDescription(item);
        plugin.sqlManager.logOffer(shopType, owner, location, item.getAmount(), price, desc);
    }
}
