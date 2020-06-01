/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetDeleteTable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AssetDelete extends DerivedEntity {

    private long id;
    private long assetId;
    private long accountId;
    private long quantityATU;
    private int timestamp;

    public AssetDelete(Transaction transaction, long assetId, long quantityATU, int timestamp, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.setDbKey(AssetDeleteTable.deleteDbKeyFactory.newKey(this.id));
        this.assetId = assetId;
        this.accountId = transaction.getSenderId();
        this.quantityATU = quantityATU;
        this.timestamp = timestamp;
    }

    /**
     * For unit tests
     */
    public AssetDelete(long id, long assetId, long senderAccountId, long quantityATU, int timestamp, int height) {
        super(null, height);
        this.id = id;
        this.assetId = assetId;
        this.accountId = senderAccountId;
        this.quantityATU = quantityATU;
        this.timestamp = timestamp;
    }

    public AssetDelete(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        setDbKey(dbKey);
        this.assetId = rs.getLong("asset_id");
        this.accountId = rs.getLong("account_id");
        this.quantityATU = rs.getLong("quantity");
        this.timestamp = rs.getInt("timestamp");
    }

}
