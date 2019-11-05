/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 *
 * @author Serhiy Lymar
 */
public class DexTradeEntryMapper implements RowMapper<DexTradeEntry> {
    
    @Override
    public DexTradeEntry map(ResultSet rs, StatementContext ctx) throws SQLException {
        DexTradeEntry dexTradeEntry = new DexTradeEntry(rs); // set dbId + height
        dexTradeEntry.setTransactionID(rs.getLong("transaction_id"));
        dexTradeEntry.setSenderOfferID(rs.getLong("sender_offer_id"));
        dexTradeEntry.setReceiverOfferID(rs.getLong("receiver_offer_id"));
        dexTradeEntry.setSenderOfferType(rs.getByte("sender_offer_type"));
        dexTradeEntry.setSenderOfferCurrency(rs.getByte("sender_offer_currency"));
        dexTradeEntry.setSenderOfferAmount(rs.getLong("sender_offer_amount"));
        dexTradeEntry.setPairCurrency(rs.getByte("pair_currency"));
        dexTradeEntry.setPairRate( EthUtil.gweiToEth(rs.getLong("pair_rate")));
        dexTradeEntry.setFinishTime(rs.getLong("finish_time"));
        return dexTradeEntry;
    }

    
}
