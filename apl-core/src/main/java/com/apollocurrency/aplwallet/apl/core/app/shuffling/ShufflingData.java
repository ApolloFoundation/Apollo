package com.apollocurrency.aplwallet.apl.core.app.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingDataTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShufflingData {

    private long shufflingId;
    private long accountId;
    private DbKey dbKey;
    private byte[][] data;
    private int transactionTimestamp;
    private int height;

    public ShufflingData(long shufflingId, long accountId, byte[][] data, int transactionTimestamp, int height) {
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.dbKey = ShufflingDataTable.dbKeyFactory.newKey(shufflingId, accountId);
        this.data = data;
        this.transactionTimestamp = transactionTimestamp;
        this.height = height;
    }

    public ShufflingData(ResultSet rs, DbKey dbKey) throws SQLException {
        this.shufflingId = rs.getLong("shuffling_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = dbKey;
        this.data = DbUtils.getArray(rs, "data", byte[][].class, Convert.EMPTY_BYTES);
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
        this.height = rs.getInt("height");
    }

}
