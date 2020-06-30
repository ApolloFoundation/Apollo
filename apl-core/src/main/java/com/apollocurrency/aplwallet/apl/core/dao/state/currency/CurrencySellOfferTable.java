/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

public class CurrencySellOfferTable extends VersionedDeletableEntityDbTable<CurrencySellOffer> {

    public static final LongKeyFactory<CurrencySellOffer> sellOfferDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(CurrencySellOffer offer) {
            if (offer.getDbKey() == null) {
                offer.setDbKey(super.newKey(offer.getId()));
            }
            return offer.getDbKey();
        }
    };

    public CurrencySellOfferTable() {
        super("sell_offer", sellOfferDbKeyFactory);
    }

    @Override
    public CurrencySellOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencySellOffer(rs, dbKey);
    }

    public void save(Connection con, CurrencySellOffer currencyBuyOffer) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, transaction_index, transaction_height, height, latest, deleted) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, currencyBuyOffer.getId());
            pstmt.setLong(++i, currencyBuyOffer.getCurrencyId());
            pstmt.setLong(++i, currencyBuyOffer.getAccountId());
            pstmt.setLong(++i, currencyBuyOffer.getRateATM());
            pstmt.setLong(++i, currencyBuyOffer.getLimit());
            pstmt.setLong(++i, currencyBuyOffer.getSupply());
            pstmt.setInt(++i, currencyBuyOffer.getExpirationHeight());
            pstmt.setInt(++i, currencyBuyOffer.getCreationHeight());
            pstmt.setShort(++i, currencyBuyOffer.getTransactionIndex());
            pstmt.setInt(++i, currencyBuyOffer.getTransactionHeight());
            pstmt.setInt(++i, currencyBuyOffer.getHeight());
            pstmt.executeUpdate();
        }
    }
}
