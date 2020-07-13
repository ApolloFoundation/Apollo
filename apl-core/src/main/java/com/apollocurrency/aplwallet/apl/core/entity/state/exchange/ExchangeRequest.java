/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.exchange;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeRequestTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeSell;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ExchangeRequest extends DerivedEntity {

    private long id;
    private long accountId;
    private long currencyId;
    private long units;
    private long rate;
    private boolean isBuy;
    private int timestamp;

    public ExchangeRequest(Transaction transaction, MonetarySystemExchangeBuyAttachment attachment, int timestamp, int height) {
        this(transaction, attachment, true, timestamp, height);
    }

    public ExchangeRequest(Transaction transaction, MonetarySystemExchangeSell attachment, int timestamp, int height) {
        this(transaction, attachment, false, timestamp, height);
    }

    public ExchangeRequest(Transaction transaction, MonetarySystemExchangeAttachment attachment, boolean isBuy, int timestamp, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.setDbKey(ExchangeRequestTable.exchangeRequestDbKeyFactory.newKey(this.id));
        this.accountId = transaction.getSenderId();
        this.currencyId = attachment.getCurrencyId();
        this.units = attachment.getUnits();
        this.rate = attachment.getRateATM();
        this.isBuy = isBuy;
        this.timestamp = timestamp;
    }

    public ExchangeRequest(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.setDbKey(dbKey);
        this.accountId = rs.getLong("account_id");
        this.currencyId = rs.getLong("currency_id");
        this.units = rs.getLong("units");
        this.rate = rs.getLong("rate");
        this.isBuy = rs.getBoolean("is_buy");
        this.timestamp = rs.getInt("timestamp");
        super.setHeight(rs.getInt("height"));
    }

    public ExchangeRequest(long id, long accountId, long currencyId, long units,
                           long rate, boolean isBuy, int height, int timestamp) {
        super(null, height);
        this.id = id;
        this.accountId = accountId;
        this.currencyId = currencyId;
        this.setHeight(height);
        this.timestamp = timestamp;
        this.units = units;
        this.rate = rate;
        this.isBuy = isBuy;
    }

}
