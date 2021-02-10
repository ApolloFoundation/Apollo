/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTransferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
@Setter
@ToString
public class CurrencyTransfer extends DerivedEntity {

    private long id;
    private long currencyId;
    private long senderId;
    private long recipientId;
    private long units;
    private int timestamp;
    private int height;

    public CurrencyTransfer(Transaction transaction, MonetarySystemCurrencyTransfer attachment, int timestamp, int height) {
        super(null, height);
        this.id = transaction.getId();
        super.setDbKey(CurrencyTransferTable.currencyTransferDbKeyFactory.newKey(this.id));
        this.currencyId = attachment.getCurrencyId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.units = attachment.getUnits();
        this.timestamp = timestamp;
        this.height = height;
    }

    /**
     * for unit tests
     */
    public CurrencyTransfer(long id, long currencyId, long senderId, long recipientId, long units, int timestamp, int height) {
        super(null, height);
        this.id = id;
        super.setDbKey(CurrencyTransferTable.currencyTransferDbKeyFactory.newKey(this.id));
        this.currencyId = currencyId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.units = units;
        this.timestamp = timestamp;
        this.height = height;
    }

    public CurrencyTransfer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        super.setDbKey(dbKey);
        this.currencyId = rs.getLong("currency_id");
        this.senderId = rs.getLong("sender_id");
        this.recipientId = rs.getLong("recipient_id");
        this.units = rs.getLong("units");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

}
