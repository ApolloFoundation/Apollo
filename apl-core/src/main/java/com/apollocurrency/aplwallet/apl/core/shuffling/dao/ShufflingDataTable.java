/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LinkKey;
import com.apollocurrency.aplwallet.apl.core.db.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingDataMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class ShufflingDataTable extends PrunableDbTable<ShufflingData> {
    private static final LinkKeyFactory<ShufflingData> KEY_FACTORY = new LinkKeyFactory<>("shuffling_id", "account_id") {
        @Override
        public DbKey newKey(ShufflingData shufflingData) {
            if (shufflingData.getDbKey() == null) {
                shufflingData.setDbKey(new LinkKey(shufflingData.getShufflingId(), shufflingData.getAccountId()));
            }
            return shufflingData.getDbKey();
        }
    };

    private static final String TABLE_NAME = "shuffling_data";
    private static final ShufflingDataMapper MAPPER = new ShufflingDataMapper(KEY_FACTORY);

    public ShufflingDataTable() {
        super(TABLE_NAME, KEY_FACTORY, false, null, false);
    }

    @Override
    protected ShufflingData load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    @Override
    public boolean isScanSafe() {
        return false; // shuffling data cannot be recovered from transactions (only by downloading/generating blocks)
    }

    public ShufflingData get(long shufflingId, long accountId) {
        return get(KEY_FACTORY.newKey(shufflingId, accountId));
    }

    @Override
    public void save(Connection con, ShufflingData entity) throws SQLException {
        try (
                PreparedStatement pstmt = con.prepareStatement("INSERT INTO shuffling_data (shuffling_id, account_id, data, "
                        + "transaction_timestamp, height) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, entity.getShufflingId());
            pstmt.setLong(++i, entity.getAccountId());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, entity.getData());
            pstmt.setInt(++i, entity.getTransactionTimestamp());
            pstmt.setInt(++i, entity.getHeight());
            pstmt.executeUpdate();
        }

    }
}
