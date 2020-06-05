/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySellOfferTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
@Setter
@ToString(callSuper = true)
public class CurrencySellOffer extends CurrencyExchangeOffer {

    public CurrencySellOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment, int height) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getBuyRateATM(),
            attachment.getTotalBuyLimit(), attachment.getInitialBuySupply(), attachment.getExpirationHeight(), transaction.getHeight(),
            transaction.getIndex(), height);
        super.setDbKey( CurrencySellOfferTable.buyOfferDbKeyFactory.newKey(this.getId()));
    }

    /**
     * for unit test
     */
    public CurrencySellOffer(long id, long currencyId, long accountId, long rateATM, long limit, long supply,
                             int expirationHeight, int transactionHeight, short transactionIndex, int height) {
        super(id, currencyId, accountId, rateATM, limit, supply, expirationHeight, transactionHeight,
            transactionIndex, height);
    }

    public CurrencySellOffer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        super.setDbKey(dbKey);
    }

}
