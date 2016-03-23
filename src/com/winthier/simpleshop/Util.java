package com.winthier.simpleshop;

import java.util.Calendar;
import java.util.GregorianCalendar;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONValue;

public class Util {
    public static String format(String msg, Object... params) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        msg = String.format(msg, params);
        return msg;
    }

    public static void sendMessage(CommandSender sender, String msg, Object... params) {
        msg = format(msg, params);
        sender.sendMessage(msg);
    }

    public static String capitalName(String in) {
        return "" + Character.toUpperCase(in.charAt(0)) + in.substring(1, in.length()).toLowerCase();
    }

    public static String niceEnumName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder(capitalName(parts[0]));
        for (int i = 1; i < parts.length; ++i) {
            sb.append(" ").append(capitalName(parts[i]));
        }
        return sb.toString();
    }

    public static String getItemName(ItemStack item) {
        ItemInfo info = Items.itemByStack(item);
        if (info == null) {
            return niceEnumName(item.getType().name());
        }
        return info.getName();
    }

    public static String getItemName(Material mat) {
        ItemInfo info = Items.itemByType(mat);
        if (info == null) {
            return niceEnumName(mat.name());
        }
        return info.getName();
    }

    /**
     * Helper function to create the genitive case of a player
     * obeying apostrophy rules. Examples:
     */
    public static String genitiveName(String name) {
        return name + "'s";
    }

    public static String getTimeString() {
        Calendar cal = GregorianCalendar.getInstance();
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                             cal.get(Calendar.YEAR),
                             cal.get(Calendar.MONTH) + 1,
                             cal.get(Calendar.DAY_OF_MONTH),
                             cal.get(Calendar.HOUR_OF_DAY),
                             cal.get(Calendar.MINUTE),
                             cal.get(Calendar.SECOND));
    }

    public static void tellRaw(Player player, Object json) {
        if (json instanceof String) {
            player.sendMessage((String)json);
            return;
        }
        String arg = JSONValue.toJSONString(json);
        String cmd = "tellraw " + player.getName() + " " + arg;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
    }

    public static String truncate(String string, int length) {
        if (string.length() > length) string = string.substring(0, length);
        return string;
    }
}
