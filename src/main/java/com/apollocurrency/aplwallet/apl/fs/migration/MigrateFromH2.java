/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.fs.migration;

import com.apollocurrency.aplwallet.apl.db.BasicDb;
import com.apollocurrency.aplwallet.apl.db.DbUtils;
import com.apollocurrency.aplwallet.apl.fs.FileManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MigrateFromH2 {

    public static void migrate(BasicDb db) {
        Connection con = null;
        Statement stmt = null;
        try {
            con = db.getConnection();
            stmt = con.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM TAGGED_DATA");
            while (rs.next()) {

                String name = rs.getString("name");
                long id = rs.getLong("id");
                byte[] data = rs.getBytes("data");

                String key = FileManager.generateKey(id);
                if(!FileManager.getStorage().put(key, data)) {
                    System.out.println("Unable to migrate file '" + name + "' with id=" + id); // id is transactionId here
                }

            }
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.close(stmt, con);
        }
    }

}
