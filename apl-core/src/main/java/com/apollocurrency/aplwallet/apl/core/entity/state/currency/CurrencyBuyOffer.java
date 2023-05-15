/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@ToString(callSuper = true)
public class CurrencyBuyOffer extends CurrencyExchangeOffer {

    public CurrencyBuyOffer(Transaction transaction, MSPublishExchangeOfferAttachment attachment, int height) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getBuyRateATM(),
            attachment.getTotalBuyLimit(), attachment.getInitialBuySupply(), attachment.getExpirationHeight(), transaction.getHeight(),
            transaction.getIndex(), height);
        super.setDbKey(CurrencyBuyOfferTable.buyOfferDbKeyFactory.newKey(this.getId()));
    }

    /**
     * for unit test
     */
    public CurrencyBuyOffer(long id, long currencyId, long accountId, long rateATM, long limit, long supply,
                            int expirationHeight, int transactionHeight, short transactionIndex, int height) {
        super(id, currencyId, accountId, rateATM, limit, supply, expirationHeight, transactionHeight,
            transactionIndex, height);
    }

    public CurrencyBuyOffer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        super.setDbKey(dbKey);
    }

}
