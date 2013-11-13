package com.winthier.simpleshop.sql;

import com.winthier.libsql.SQLRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.enchantments.Enchantment;

public class ClearOffersRequest implements SQLRequest {
        ClearOffersRequest() {}

        @Override
        public void execute(Connection c) throws SQLException {
                Statement s;
                s = c.createStatement();
                s.execute(" DELETE FROM `simpleshop_offers` WHERE `version` <> (SELECT `version` FROM `simpleshop_version` WHERE `name` = 'offers')");
                s.close();
        }
}
