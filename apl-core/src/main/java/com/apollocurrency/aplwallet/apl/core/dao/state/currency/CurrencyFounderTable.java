/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CurrencyFounderTable extends VersionedDeletableEntityDbTable<CurrencyFounder> {

    public static final LinkKeyFactory<CurrencyFounder> currencyFounderDbKeyFactory = new LinkKeyFactory<>("currency_id", "account_id") {
        @Override
        public DbKey newKey(CurrencyFounder currencyFounder) {
            if (currencyFounder.getDbKey() == null) {
                currencyFounder.setDbKey(super.newKey(currencyFounder.getCurrencyId(), currencyFounder.getAccountId()));
            }
            return currencyFounder.getDbKey();
        }
    };

    public CurrencyFounderTable() {
        super("currency_founder", currencyFounderDbKeyFactory);
    }

    @Override
    public CurrencyFounder load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencyFounder(rs, dbKey);
    }

    @Override
    public void save(Connection con, CurrencyFounder currencyFounder) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_founder (currency_id, account_id, amount, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE "
                + "currency_id = VALUES(currency_id), account_id = VALUES(account_id), amount = VALUES(amount), "
                + "height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, currencyFounder.getCurrencyId());
            pstmt.setLong(++i, currencyFounder.getAccountId());
            pstmt.setLong(++i, currencyFounder.getAmountPerUnitATM());
            pstmt.setInt(++i, currencyFounder.getHeight());
            pstmt.executeUpdate();
        }
    }



}
