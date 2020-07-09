/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.exchange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;

public class ExchangeRequestTable extends EntityDbTable<ExchangeRequest> {

    public static final LongKeyFactory<ExchangeRequest> exchangeRequestDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(ExchangeRequest exchangeRequest) {
            if (exchangeRequest.getDbKey() == null) {
                exchangeRequest.setDbKey(super.newKey(exchangeRequest.getId()));
            }
            return exchangeRequest.getDbKey();
        }
    };

    public ExchangeRequestTable() {
        super("exchange_request", exchangeRequestDbKeyFactory);
    }

    @Override
    public ExchangeRequest load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new ExchangeRequest(rs, dbKey);
    }

    @Override
    public void save(Connection con, ExchangeRequest exchangeRequest) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO exchange_request (id, account_id, currency_id, "
            + "units, rate, is_buy, timestamp, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, exchangeRequest.getId());
            pstmt.setLong(++i, exchangeRequest.getAccountId());
            pstmt.setLong(++i, exchangeRequest.getCurrencyId());
            pstmt.setLong(++i, exchangeRequest.getUnits());
            pstmt.setLong(++i, exchangeRequest.getRate());
            pstmt.setBoolean(++i, exchangeRequest.isBuy());
            pstmt.setInt(++i, exchangeRequest.getTimestamp());
            pstmt.setInt(++i, exchangeRequest.getHeight());
            pstmt.executeUpdate();
        }
    }
}
