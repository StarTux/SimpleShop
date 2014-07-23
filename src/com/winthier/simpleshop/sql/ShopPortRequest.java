package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.ShopType;
import com.winthier.simpleshop.SimpleShopPlugin;
import com.winthier.simpleshop.Util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ShopPortRequest extends BukkitRunnable implements SQLRequest {
    private final SimpleShopPlugin plugin;
    private final Player player;
    private String ownerName;
    // State
    private String worldName = null;
    private boolean success = false;
    private int x, z;

    public ShopPortRequest(SimpleShopPlugin plugin, Player player, String ownerName) {
        this.plugin = plugin;
        this.player = player;
        this.ownerName = ownerName;
    }

    @Override
    public void execute(Connection c) throws SQLException {
        PreparedStatement s;
        StringBuilder sb = new StringBuilder();
        sb.append(" SELECT `owner`, `world`, `x`, `z` FROM `simpleshop_offers`");
        sb.append(" WHERE `version` = (");
        sb.append("   SELECT `version` FROM `simpleshop_version`");
        sb.append("   WHERE `name` = 'offers'");
        sb.append(" )");
        sb.append(" AND `owner` = ?");
        s = c.prepareStatement(sb.toString());
        s.setString(1, ownerName);
        // Execute
        ResultSet result = s.executeQuery();
        int x = 0;
        int z = 0;
        int size = 0;
        while (result.next()) {
            ownerName = result.getString("owner");
            if (worldName == null) {
                worldName = result.getString("world");
            }
            if (worldName.equals(result.getString("world"))) {
                success = true;
                size += 1;
                x += result.getInt("x");
                z += result.getInt("z");
            }
        }
        s.close();
        if (size > 0) {
            x /= size;
            z /= size;
        }
        this.x = x;
        this.z = z;

        runTask(plugin);
    }

    private int hdist(int x1, int z1, int x2, int z2) {
        int a = x1 - x2;
        int b = z1 - z2;
        return a*a + b*b;
    }

    @Override
    public void run() {
        if (!success) {
            Util.sendMessage(player, "&cShop not found: %s", ownerName);
            return;
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return;
        Chunk chunk;
        chunk = world.getBlockAt(x, 64, z).getChunk();
        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();
        Block result = null;
        int dist = 0;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                chunk = world.getChunkAt(chunkX + dx, chunkZ + dz);
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Sign) {
                        if (checkSign((Sign)state)) {
                            Block block = state.getBlock();
                            if (result == null) {
                                result = block;
                                dist = hdist(x, z, block.getX(), block.getZ());
                            } else {
                                int dist2 = hdist(x, z, block.getX(), block.getZ());
                                if (dist2 < dist) {
                                    result = block;
                                    dist = dist2;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (result == null) {
            Util.sendMessage(player, "&cCan't port you to %s's shop :(", ownerName);
        } else {
            Location loc = result.getLocation().add(0.5, 0.0, 0.5);
            Vector dir = new Vector(x - result.getX(), 0, z - result.getZ()).normalize();
            loc.setDirection(dir);
            player.teleport(loc);
            Util.sendMessage(player, "&bTeleported to %s's shop.", ownerName);
        }
    }

    private final String line1 = Util.format("&f&l");
    private final String line2 = Util.format("&f");
    private final String line3 = Util.format("&f");
    private final String line4 = Util.format("&f&m   ");
    private boolean checkSign(Sign sign) {
        if (!sign.getLine(0).startsWith(line1)) return false;
        if (!sign.getLine(1).startsWith(line2)) return false;
        if (!sign.getLine(2).startsWith(line3)) return false;
        if (!sign.getLine(3).startsWith(line4)) return false;
        if (sign.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) return false;
        return true;
    }

    // private final int RADIUS = 7;
    // @Override
    // public void run() {
    //     if (!success) {
    //         Util.sendMessage(player, "&cShop not found: %s", ownerName);
    //         return;
    //     }
    //     World world = plugin.getServer().getWorld(worldName);
    //     if (world == null) return;
    //     Block result = null;
    //     for (int dx = -RADIUS; dx <= RADIUS; ++dx) {
    //         for (int dz = -RADIUS; dz <= RADIUS; ++dz) {
    //             Block block = world.getHighestBlockAt(x + dx, z + dz);
    //             if (isSave(block)) {
    //                 if (result == null || block.getY() < result.getY()) {
    //                     result = block;
    //                 } else if (block.getY() == result.getY()) {
    //                     int tmpx, tmpz;
    //                     tmpx = result.getX() - x;
    //                     tmpz = result.getZ() - z;
    //                     int dist = tmpx*tmpx + tmpz*tmpz;
    //                     tmpx = block.getX() - x;
    //                     tmpz = block.getZ() - z;
    //                     int dist2 = tmpx*tmpx + tmpz*tmpz;
    //                     if (dist2 < dist) result = block;
    //                 }
    //             }
    //         }
    //     }
    //     if (result == null) {
    //         Util.sendMessage(player, "&cCan't port you to %s's shop :(", ownerName);
    //         return;
    //     }
    //     Location loc = result.getLocation().add(0.5, 0.0, 0.5);
    //     Vector dir = new Vector(x - result.getX(), 0, z - result.getZ()).normalize();
    //     loc.setDirection(dir);
    //     player.teleport(loc);
    //     Util.sendMessage(player, "&bTeleported to %s's shop.", ownerName);
    // }
    //
    // private boolean isSave(Block block) {
    //     if (!block.getRelative(BlockFace.DOWN).getType().isSolid()) return false;
    //     for (int i = 0; i < 8; ++i) {
    //         if (block.getRelative(BlockFace.UP, i).getType() != Material.AIR) return false;
    //     }
    //     return true;
    // }
}
