/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        try (PreparedStatement pstmt = databaseManager.getDataSource().getConnection()
                .prepareStatement("INSERT INTO dex_offer (transaction_id, account_id, type, "
                + "offer_currency, offer_amount, pair_currency, pair_rate, finish_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, dexOffer.getTransactionId());
            pstmt.setLong(++i, dexOffer.getAccountId());
            pstmt.setInt(++i, dexOffer.getType().ordinal());
            pstmt.setInt(++i, dexOffer.getOfferCurrency().ordinal());
            pstmt.setLong(++i, dexOffer.getOfferAmount());
            pstmt.setInt(++i, dexOffer.getPairCurrency().ordinal());
            pstmt.setLong(++i, dexOffer.getPairRate());
            pstmt.setInt(++i, dexOffer.getFinishTime());

            pstmt.executeUpdate();
         } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.toString(), e);
         }

    }

    public List<DexOffer> getOffers(DexOfferDBRequest dexOfferDBRequest){
        List<DexOffer> offers = new ArrayList<>();

        try (PreparedStatement pstmt = databaseManager.getDataSource().getConnection()
                .prepareStatement("SELECT * FROM dex_offer AS offer " +
                        "WHERE offer.FINISH_TIME > ? AND offer.type = ? AND " +
                        "offer.OFFER_CURRENCY = ? AND offer.PAIR_CURRENCY= ? " +
                        " ORDER BY offer.pair_rate")) {

            pstmt.setInt(1, dexOfferDBRequest.getCurrentTime());
            pstmt.setInt(2, dexOfferDBRequest.getType().ordinal());
            pstmt.setInt(3, dexOfferDBRequest.getOfferCur().ordinal());
            pstmt.setInt(4, dexOfferDBRequest.getPairCur().ordinal());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                   offers.add(loadBlock(rs));
                }
            }

        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.toString(), e);
        }

        return offers;
    }


    private DexOffer loadBlock(ResultSet rs) {
        DexOffer dexOffer = new DexOffer();
        try {
            dexOffer.setTransactionId(rs.getLong("transaction_id"));
            dexOffer.setAccountId(rs.getLong("account_id"));
            dexOffer.setType(OfferType.getType(rs.getInt("type")));
            dexOffer.setOfferCurrency(DexCurrencies.getType(rs.getInt("offer_currency")));
            dexOffer.setOfferAmount(rs.getLong("offer_amount"));
            dexOffer.setPairCurrency(DexCurrencies.getType(rs.getInt("pair_currency")));
            dexOffer.setPairRate(rs.getLong("pair_currency"));
            dexOffer.setFinishTime(rs.getInt("finish_time"));

        } catch (SQLException e){
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.toString(), e);
        }

        return dexOffer;
    }



}
