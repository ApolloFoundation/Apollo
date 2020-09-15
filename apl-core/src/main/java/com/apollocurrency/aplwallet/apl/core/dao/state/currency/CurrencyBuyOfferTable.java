/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class CurrencyBuyOfferTable extends VersionedDeletableEntityDbTable<CurrencyBuyOffer> {

    public static final LongKeyFactory<CurrencyBuyOffer> buyOfferDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(CurrencyBuyOffer offer) {
            if (offer.getDbKey() == null) {
                offer.setDbKey(super.newKey(offer.getId()));
            }
            return offer.getDbKey();
        }
    };

    @Inject
    public CurrencyBuyOfferTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                 DatabaseManager databaseManager,
                                 Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("buy_offer", buyOfferDbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public CurrencyBuyOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencyBuyOffer(rs, dbKey);
    }

    public void save(Connection con, CurrencyBuyOffer buyOffer) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, transaction_index, transaction_height, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), currency_id = VALUES(currency_id), account_id = VALUES(account_id), "
                + "rate = VALUES(rate), unit_limit = VALUES(unit_limit), supply = VALUES(supply), expiration_height = VALUES(expiration_height), "
                + "creation_height = VALUES(creation_height), transaction_index = VALUES(transaction_index), "
                + "transaction_height = VALUES(transaction_height), height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, buyOffer.getId());
            pstmt.setLong(++i, buyOffer.getCurrencyId());
            pstmt.setLong(++i, buyOffer.getAccountId());
            pstmt.setLong(++i, buyOffer.getRateATM());
            pstmt.setLong(++i, buyOffer.getLimit());
            pstmt.setLong(++i, buyOffer.getSupply());
            pstmt.setInt(++i, buyOffer.getExpirationHeight());
            pstmt.setInt(++i, buyOffer.getCreationHeight());
            pstmt.setShort(++i, buyOffer.getTransactionIndex());
            pstmt.setInt(++i, buyOffer.getTransactionHeight());
            pstmt.setInt(++i, buyOffer.getHeight());
            pstmt.executeUpdate();
        }
    }
}
