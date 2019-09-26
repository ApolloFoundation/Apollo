/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;


import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexTradeEntryMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;

/**
 * DEX Trade data in derived table hierarchy
 */
@Singleton
public class DexTradeTable extends VersionedDeletableEntityDbTable<DexTradeEntry> {

    static final LongKeyFactory<DexTradeEntry> KEY_FACTORY = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(DexTradeEntry tradeEntry) {
            return new LongKey(tradeEntry.getTransactionID());
        }
    };

    private static final String TABLE_NAME = "dex_trade";
    private DexTradeEntryMapper mapper = new DexTradeEntryMapper();

    @Inject
    public DexTradeTable() {
        super(TABLE_NAME, KEY_FACTORY, false);
    }

    @Override
    protected DexTradeEntry load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return mapper.map(rs, null);
    }

    @Override
    public void save(Connection con, DexTradeEntry entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO dex_trade(TRANSACTION_ID, " +
                "SENDER_OFFER_ID, RECEIVER_OFFER_ID, SENDER_OFFER_TYPE, " +
                "SENDER_OFFER_CURRENCY, SENDER_OFFER_AMOUNT, PAIR_CURRENCY, " +
                "PAIR_RATE, FINISH_TIME, HEIGHT ) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, entity.getTransactionID());
            pstmt.setLong(++i, entity.getSenderOfferID());
            pstmt.setLong(++i, entity.getReceiverOfferID());
            pstmt.setByte(++i, entity.getSenderOfferType());
            pstmt.setByte(++i, entity.getSenderOfferCurrency());
            pstmt.setLong(++i, entity.getSenderOfferAmount());
            pstmt.setByte(++i, entity.getPairCurrency());
            pstmt.setBigDecimal(++i, entity.getPairRate());
            pstmt.setInt(++i, entity.getFinishTime());
            pstmt.setInt(++i, entity.getHeight());

            pstmt.executeUpdate();
        }
    }
}
