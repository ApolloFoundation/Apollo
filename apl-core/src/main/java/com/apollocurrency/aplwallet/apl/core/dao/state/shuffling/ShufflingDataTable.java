/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.app.shuffling.ShufflingData;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ShufflingDataTable extends PrunableDbTable<ShufflingData> {

    public static final LinkKeyFactory<ShufflingData> dbKeyFactory = new LinkKeyFactory<>("shuffling_id", "account_id") {
        @Override
        public DbKey newKey(ShufflingData shufflingData) {
            return shufflingData.getDbKey();
        }
    };

    @Inject
    public ShufflingDataTable() {
        super("shuffling_data", dbKeyFactory);
    }


    @Override
    public boolean isScanSafe() {
        return false; // shuffling data cannot be recovered from transactions (only by downloading/generating blocks)
    }

    @Override
    public ShufflingData load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new ShufflingData(rs, dbKey);
    }

    @Override
    public void save(Connection con, ShufflingData shufflingData) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.SET_ARRAY)
            PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO shuffling_data (shuffling_id, account_id, data, "
                    + "transaction_timestamp, height) "
                    + "VALUES (?, ?, ?, ?, ?)")
        ) {
            int i = 0;
            pstmt.setLong(++i, shufflingData.getShufflingId());
            pstmt.setLong(++i, shufflingData.getAccountId());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, shufflingData.getData());
            pstmt.setInt(++i, shufflingData.getTransactionTimestamp());
            pstmt.setInt(++i, shufflingData.getHeight());
            pstmt.executeUpdate();
        }
    }


    public byte[][] getData(long shufflingId, long accountId) {
        ShufflingData shufflingData = get(dbKeyFactory.newKey(shufflingId, accountId));
        return shufflingData != null ? shufflingData.getData() : null;
    }

    public void restoreData(long shufflingId, long accountId, byte[][] data, int timestamp, int height) {
        if (data != null && getData(shufflingId, accountId) == null) {
            insert(new ShufflingData(shufflingId, accountId, data, timestamp, height));
        }
    }

}
