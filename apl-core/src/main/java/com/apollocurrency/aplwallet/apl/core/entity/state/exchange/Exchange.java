/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.exchange;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
public class Exchange extends DerivedEntity {

    private long transactionId;
    private long currencyId;
    private long blockId;
    private long offerId;
    private long sellerId;
    private long buyerId;
    private long units;
    private long rate;
    private int timestamp;

    public Exchange(long transactionId, long currencyId, long blockId, long offerId,
                     long sellerId, long buyerId, long units, long rateATM, int timestamp, int height) {
        super(null, height);
        this.transactionId = transactionId;
        this.currencyId = currencyId;
        this.blockId = blockId;
        this.offerId = offerId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.setDbKey(ExchangeTable.exchangeDbKeyFactory.newKey(this.transactionId, this.offerId));
        this.units = units;
        this.rate = rateATM;
        this.timestamp = timestamp;
    }

    public Exchange(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.transactionId = rs.getLong("transaction_id");
        this.currencyId = rs.getLong("currency_id");
        this.blockId = rs.getLong("block_id");
        this.offerId = rs.getLong("offer_id");
        this.sellerId = rs.getLong("seller_id");
        this.buyerId = rs.getLong("buyer_id");
        this.setDbKey(dbKey);
        this.units = rs.getLong("units");
        this.rate = rs.getLong("rate");
        this.timestamp = rs.getInt("timestamp");
    }

}
