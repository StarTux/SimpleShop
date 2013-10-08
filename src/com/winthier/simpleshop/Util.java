package com.winthier.simpleshop;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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
}
