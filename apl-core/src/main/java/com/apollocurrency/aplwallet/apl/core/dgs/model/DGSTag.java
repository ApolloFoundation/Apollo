/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.StringKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedEntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DGSTag {
    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }
    
        private final String tag;
        private DbKey dbKey;
        private int inStockCount;
        private int totalCount;
    private int height;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public DGSTag(String tag) {
            this.tag = tag;
        }

    public DGSTag(String tag, int inStockCount, int totalCount, int height) {
        this.tag = tag;
        this.inStockCount = inStockCount;
        this.totalCount = totalCount;
        this.height = height;
    }

    public DGSTag(ResultSet rs, DbKey dbKey) throws SQLException {
            this.tag = rs.getString("tag");
            this.dbKey = dbKey;
            this.inStockCount = rs.getInt("in_stock_count");
            this.totalCount = rs.getInt("total_count");
            this.height = rs.getInt("height");
        }

        public void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO tag (tag, in_stock_count, total_count, height, latest) "
                    + "KEY (tag, height) VALUES (?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setString(++i, this.tag);
                pstmt.setInt(++i, this.inStockCount);
                pstmt.setInt(++i, this.totalCount);
                pstmt.setInt(++i, this.height);
                pstmt.executeUpdate();
            }
        }

        public String getTag() {
            return tag;
        }

        public int getInStockCount() {
            return inStockCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

}
