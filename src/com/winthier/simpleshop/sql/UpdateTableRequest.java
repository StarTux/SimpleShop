package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import com.winthier.simpleshop.SimpleShopPlugin;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public class UpdateTableRequest implements SQLRequest {
        UpdateTableRequest() {}

        @Override
        public void execute(Connection c) throws SQLException {
                Statement s;

                // Add material column
                s = c.createStatement();
                s.execute("ALTER TABLE `simpleshop_transactions` ADD COLUMN `material` VARCHAR(32) NOT NULL AFTER `itemid`");
                s.close();
                s = c.createStatement();
                s.execute("ALTER TABLE `simpleshop_transactions` DROP KEY `itemid`");
                s.close();
                s = c.createStatement();
                s.execute("ALTER TABLE `simpleshop_transactions` ADD KEY (`material`)");
                s.close();

                // Translate material

                {
                        s = c.createStatement();
                        s.execute(" CREATE TEMPORARY TABLE IF NOT EXISTS `materials` (" +
                                  " `id` INTEGER(11) UNSIGNED NOT NULL," +
                                  " `name` VARCHAR(32) NOT NULL," +
                                  " PRIMARY KEY (`id`)," +
                                  " KEY (`name`)" +
                                  ") ENGINE=MyISAM");
                        s.close();
                        StringBuilder sb = new StringBuilder("INSERT IGNORE INTO `materials` (`id`, `name`) VALUES (0, 'air')");
                        for (Material mat : Material.values()) {
                                sb.append(", (").append(mat.getId()).append(", '").append(mat.name().toLowerCase()).append("')");
                        }
                        s = c.createStatement();
                        s.execute(sb.toString());
                        s.close();
                }

                s = c.createStatement();
                s.execute("UPDATE `simpleshop_transactions` SET `material` = (SELECT `name` from `materials` WHERE `id` = `itemid`)");
                s.close();

                s = c.createStatement();
                s.execute("ALTER TABLE `simpleshop_transactions` DROP COLUMN `itemid`");
                s.close();
        }
}
