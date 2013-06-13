package com.winthier.simpleshop.griefprevention;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GriefPreventionHelper {
        public static boolean canBuild(Player player, Location location) {
                GriefPrevention gp = GriefPrevention.instance;
                if (gp == null) return true;
                return gp.allowBuild(player, location) == null;
        }
}
