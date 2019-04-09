/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class DexOfferDao {
    private static final Logger LOG = getLogger(DexOfferDao.class);

    private DatabaseManager databaseManager;

    @Inject
    public DexOfferDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }


    public void save(DexOffer dexOffer){
        try (PreparedStatement pstmt = databaseManager.getDataSource().getConnection().prepareStatement("INSERT INTO dex_offer (transaction_id, account_id, type, "
                + "offer_currency, offer_amount, pair_currency, pair_rate, finish_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, dexOffer.getTransactionId());
            pstmt.setLong(++i, dexOffer.getAccountId());
            pstmt.setInt(++i, dexOffer.getType().ordinal());
            pstmt.setInt(++i, dexOffer.getOfferCurrency().ordinal());
            pstmt.setLong(++i, dexOffer.getOfferAmount());
            pstmt.setInt(++i, dexOffer.getPairCurrency().ordinal());
            pstmt.setBigDecimal(++i, dexOffer.getPairRate());
            pstmt.setInt(++i, dexOffer.getFinishTime());

            pstmt.executeUpdate();
         } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.toString(), e);
         }

    }


}
