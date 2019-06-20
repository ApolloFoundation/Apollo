/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DexOfferMapper implements RowMapper<DexOffer> {

    @Override
    public DexOffer map(ResultSet rs, StatementContext ctx) throws SQLException {
        DexOffer dexOffer = new DexOffer();

        dexOffer.setId(rs.getLong("db_id"));
        dexOffer.setTransactionId(rs.getLong("transaction_id"));
        dexOffer.setAccountId(rs.getLong("account_id"));
        dexOffer.setType(OfferType.getType(rs.getInt("type")));
        dexOffer.setOfferCurrency(DexCurrencies.getType(rs.getInt("offer_currency")));
        dexOffer.setOfferAmount(rs.getLong("offer_amount"));
        dexOffer.setPairCurrency(DexCurrencies.getType(rs.getInt("pair_currency")));
        dexOffer.setPairRate(rs.getLong("pair_rate"));
        dexOffer.setFinishTime(rs.getInt("finish_time"));
        dexOffer.setStatus(OfferStatus.getType(rs.getInt("status")));
        dexOffer.setFromAddress(rs.getString("from_address"));
        dexOffer.setToAddress(rs.getString("to_address"));

        return dexOffer;
    }

}