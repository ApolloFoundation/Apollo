/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;

public class TaggedDataTimestamp {

    private Blockchain blockchain = CDI.current().select(Blockchain.class).get();

    private final long id;
//    private final DbKey dbKey;
    private int timestamp;

    public TaggedDataTimestamp(long id, int timestamp) {
        this.id = id;
//        this.dbKey = TaggedData.timestampKeyFactory.newKey(this.id);
        this.timestamp = timestamp;
    }

    public TaggedDataTimestamp(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
//        this.dbKey = dbKey;
        this.timestamp = rs.getInt("timestamp");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement(
                "MERGE INTO tagged_data_timestamp (id, timestamp, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setInt(++i, this.timestamp);
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public long getId() {
        return id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
