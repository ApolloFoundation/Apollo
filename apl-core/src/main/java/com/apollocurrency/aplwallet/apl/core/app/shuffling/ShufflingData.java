package com.apollocurrency.aplwallet.apl.core.app.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingDataTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
@Setter
public class ShufflingData extends DerivedEntity {
    private long shufflingId;
    private long accountId;
    private byte[][] data;
    private int transactionTimestamp;

    public ShufflingData(long shufflingId, long accountId, byte[][] data, int transactionTimestamp, int height) {
        super(null, height);
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.data = data;
        this.transactionTimestamp = transactionTimestamp;
        setDbKey(ShufflingDataTable.dbKeyFactory.newKey(shufflingId, accountId));
    }

    public ShufflingData(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        setDbKey(dbKey);
        this.shufflingId = rs.getLong("shuffling_id");
        this.accountId = rs.getLong("account_id");
        this.data = DbUtils.getArray(rs, "data", byte[][].class, Convert.EMPTY_BYTES);
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
    }

}
