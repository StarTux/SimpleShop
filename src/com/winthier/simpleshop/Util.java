package com.winthier.simpleshop;

import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

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

        private static String capitalName(String in) {
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
         * Sirabell => Sirabell's
         * StarTux => StarTux'
         */
        public static String genitiveName(String name) {
                if (name.endsWith("s") || name.endsWith("x") || name.endsWith("z")) {
                        return name + "'";
                }
                return name + "'s";
        }
}
