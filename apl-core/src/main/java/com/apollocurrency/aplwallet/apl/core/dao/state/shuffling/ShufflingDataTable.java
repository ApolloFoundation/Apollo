/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class ShufflingDataTable extends PrunableDbTable<ShufflingData> {

    public static final LongKeyFactory<ShufflingData> dbKeyFactory = new LongKeyFactory<>("transaction_id") {
        @Override
        public DbKey newKey(ShufflingData shufflingData) {
            if (shufflingData.getDbKey() == null) {
                shufflingData.setDbKey(newKey(shufflingData.getTransactionId()));
            }
            return shufflingData.getDbKey();
        }
    };

    @Inject
    public ShufflingDataTable(DatabaseManager databaseManager,
                              BlockchainConfig blockchainConfig,
                              PropertiesHolder propertiesHolder,
                              Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("shuffling_data", dbKeyFactory, false,
            null, databaseManager, blockchainConfig,
            propertiesHolder, fullTextOperationDataEvent);
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
                "INSERT INTO shuffling_data (transaction_id, shuffling_id, account_id, data, "
                    + "transaction_timestamp, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?)")
        ) {
            int i = 0;
            pstmt.setLong(++i, shufflingData.getTransactionId());
            pstmt.setLong(++i, shufflingData.getShufflingId());
            pstmt.setLong(++i, shufflingData.getAccountId());
            DbUtils.set2dByteArray(pstmt, ++i, shufflingData.getData());
            pstmt.setInt(++i, shufflingData.getTransactionTimestamp());
            pstmt.setInt(++i, shufflingData.getHeight());
            pstmt.executeUpdate();
        }
    }


    public byte[][] getData(long shufflingId, long accountId) {
        ShufflingData shufflingData = getBy(new DbClause.LongClause("shuffling_id", shufflingId).and(new DbClause.LongClause("account_id", accountId)));
        return shufflingData != null ? shufflingData.getData() : null;
    }

    public byte[][] getData(long transactionId) {
        ShufflingData shufflingData = get(dbKeyFactory.newKey(transactionId));
        return shufflingData != null ? shufflingData.getData() : null;
    }

    public void restoreData(long transactionId, long shufflingId, long accountId, byte[][] data, int timestamp, int height) {
        if (data != null && getData(transactionId) == null) {
            insert(new ShufflingData(transactionId, shufflingId, accountId, data, timestamp, height));
        }
    }
}
