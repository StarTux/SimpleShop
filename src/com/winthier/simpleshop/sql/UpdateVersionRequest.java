package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.bukkit.enchantments.Enchantment;

public class UpdateVersionRequest implements SQLRequest {
        private final String name;

        UpdateVersionRequest(String name) {
                this.name = name;
        }

        @Override
        public void execute(Connection c) throws SQLException {
                PreparedStatement s;
                s = c.prepareStatement(" INSERT IGNORE INTO `simpleshop_version`" +
                                       " (`name`, `version`) " +
                                       " VALUES (?, 0)" +
                                       " ON DUPLICATE KEY" +
                                       " UPDATE `version` = `version` + 1");
                s.setString(1, name);
                s.executeUpdate();
                s.close();
        }
}
