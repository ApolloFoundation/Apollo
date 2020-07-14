/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class CurrencyExchangeOffer extends VersionedDeletableEntity {

    private final long id;
    private final long currencyId;
    private final long accountId;
    private final long rateATM;
    private final int expirationHeight;
    private int creationHeight;
    private final short transactionIndex;
    private final int transactionHeight;
    private long limit; // limit on the total sum of units for this offer across transactions
    private long supply; // total units supply for the offer

    public CurrencyExchangeOffer(long id, long currencyId, long accountId, long rateATM, long limit, long supply,
                          int expirationHeight, int transactionHeight, short transactionIndex, int height) {
        super(null, height);
        this.id = id;
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.rateATM = rateATM;
        this.limit = limit;
        this.supply = supply;
        this.expirationHeight = expirationHeight;
        this.creationHeight = height;
        this.transactionIndex = transactionIndex;
        this.transactionHeight = transactionHeight;
    }

    public CurrencyExchangeOffer(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.rateATM = rs.getLong("rate");
        this.limit = rs.getLong("unit_limit");
        this.supply = rs.getLong("supply");
        this.expirationHeight = rs.getInt("expiration_height");
        this.creationHeight = rs.getInt("creation_height");
        this.transactionIndex = rs.getShort("transaction_index");
        this.transactionHeight = rs.getInt("transaction_height");
    }

    public long increaseSupply(long delta) {
        long excess = Math.max(Math.addExact(supply, Math.subtractExact(delta, limit)), 0);
        this.supply += delta - excess;
        return excess;
    }

    public void decreaseLimitAndSupply(long delta) {
        this.limit -= delta;
        this.supply -= delta;
    }
}
