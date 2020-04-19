/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@ToString(callSuper = true)
@Getter
public class AssetDividend extends DerivedEntity {

    private long id;
    @Setter
    private long assetId;
    @Setter
    private long amountATMPerATU;
    @Setter
    private int dividendHeight;
    @Setter
    private long totalDividend;
    @Setter
    private long numAccounts;

    private int timestamp;

    public AssetDividend(long transactionId, int height, int timestamp) {
        super(null, height);
        this.id = transactionId;
        this.timestamp = timestamp;
    }

    public AssetDividend(long transactionId, ColoredCoinsDividendPayment attachment,
                         long totalDividend, long numAccounts, int height, int timestamp) {
        this(transactionId, height, timestamp);

        this.assetId = attachment.getAssetId();
        this.amountATMPerATU = attachment.getAmountATMPerATU();
        this.dividendHeight = attachment.getHeight();
        this.totalDividend = totalDividend;
        this.numAccounts = numAccounts;
    }

    public AssetDividend(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.assetId = rs.getLong("asset_id");
        this.amountATMPerATU = rs.getLong("amount");
        this.dividendHeight = rs.getInt("dividend_height");
        this.totalDividend = rs.getLong("total_dividend");
        this.numAccounts = rs.getLong("num_accounts");
        this.timestamp = rs.getInt("timestamp");
        setDbKey(dbKey);
    }
}
