package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.enchantments.Enchantment;

public class CreateTableRequest implements SQLRequest {
        CreateTableRequest() {}

        @Override
        public void execute(Connection c) throws SQLException {
                Statement s;
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE IF NOT EXISTS `simpleshop_transactions` (");
                sb.append(" `id` INT(11) NOT NULL AUTO_INCREMENT,");
                sb.append(" `time` TIMESTAMP DEFAULT NOW(),");
                sb.append(" `shop_type` ENUM('buy', 'sell') NOT NULL DEFAULT 'buy',");
                sb.append(" `player` VARCHAR(16) NOT NULL,");
                sb.append(" `owner` VARCHAR(16) DEFAULT NULL,");
                sb.append(" `price` FLOAT(11, 4) NOT NULL,");
                sb.append(" `material` VARCHAR(32) NOT NULL,");
                sb.append(" `amount` INT(11) NOT NULL,");
                sb.append(" `itemdata` INT(6) NOT NULL DEFAULT 0,");
                sb.append(" `display_name` VARCHAR(32) DEFAULT NULL,");
                for (Enchantment enchantment : Enchantment.values()) {
                        sb.append(" `");
                        sb.append("enchantment_");
                        sb.append(enchantment.getName().toLowerCase());
                        sb.append("` INT(2) NOT NULL DEFAULT 0,");
                }
                sb.append(" `lore` VARCHAR(256) DEFAULT NULL,");
                sb.append(" `world` VARCHAR(32) NOT NULL,");
                sb.append(" `x` INT(11) NOT NULL,");
                sb.append(" `y` INT(11) NOT NULL,");
                sb.append(" `z` INT(11) NOT NULL,");
                sb.append(" PRIMARY KEY (`id`),");
                sb.append(" KEY (`owner`),");
                sb.append(" KEY (`shop_type`),");
                sb.append(" KEY `location` (`world`, `x`, `y`, `z`),");
                sb.append(" KEY (`material`)");
                sb.append(") ENGINE=MyISAM");
                s = c.createStatement();
                s.execute(sb.toString());
                s.close();
        }
}
