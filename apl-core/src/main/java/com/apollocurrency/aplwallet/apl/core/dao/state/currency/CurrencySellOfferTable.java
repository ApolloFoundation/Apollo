/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
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

    @Inject
    public CurrencySellOfferTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                  DatabaseManager databaseManager,
                                  Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("sell_offer", sellOfferDbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public CurrencySellOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencySellOffer(rs, dbKey);
    }

    public void save(Connection con, CurrencySellOffer sellOffer) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, transaction_index, transaction_height, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE "
                + "id = VALUES(id), currency_id = VALUES(currency_id), account_id = VALUES(account_id), "
                + "rate = VALUES(rate), unit_limit = VALUES(unit_limit), supply = VALUES(supply), "
                + "expiration_height = VALUES(expiration_height), creation_height = VALUES(creation_height), "
                + "transaction_index = VALUES(transaction_index), transaction_height = VALUES(transaction_height), "
                + "height = VALUES(height), latest = TRUE , deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, sellOffer.getId());
            pstmt.setLong(++i, sellOffer.getCurrencyId());
            pstmt.setLong(++i, sellOffer.getAccountId());
            pstmt.setLong(++i, sellOffer.getRateATM());
            pstmt.setLong(++i, sellOffer.getLimit());
            pstmt.setLong(++i, sellOffer.getSupply());
            pstmt.setInt(++i, sellOffer.getExpirationHeight());
            pstmt.setInt(++i, sellOffer.getCreationHeight());
            pstmt.setShort(++i, sellOffer.getTransactionIndex());
            pstmt.setInt(++i, sellOffer.getTransactionHeight());
            pstmt.setInt(++i, sellOffer.getHeight());
            pstmt.executeUpdate();
        }
    }
}
