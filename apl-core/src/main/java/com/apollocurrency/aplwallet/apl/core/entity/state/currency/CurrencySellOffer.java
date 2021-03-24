/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySellOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@ToString(callSuper = true)
public class CurrencySellOffer extends CurrencyExchangeOffer {

    public CurrencySellOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment, int height) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getSellRateATM(),
            attachment.getTotalSellLimit(), attachment.getInitialSellSupply(), attachment.getExpirationHeight(), transaction.getHeight(),
            transaction.getIndex(), height);
        super.setDbKey( CurrencySellOfferTable.sellOfferDbKeyFactory.newKey(this.getId()));
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
