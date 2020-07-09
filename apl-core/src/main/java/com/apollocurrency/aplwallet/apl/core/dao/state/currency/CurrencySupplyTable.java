/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

public class CurrencySupplyTable extends VersionedDeletableEntityDbTable<CurrencySupply> {

    public static final LongKeyFactory<CurrencySupply> currencySupplyDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(CurrencySupply currencySupply) {
            if (currencySupply.getDbKey() == null) {
                currencySupply.setDbKey(super.newKey(currencySupply.getCurrencyId()));
            }
            return currencySupply.getDbKey();
        }
    };

    public CurrencySupplyTable() {
        super("currency_supply", currencySupplyDbKeyFactory);
    }

    @Override
    public CurrencySupply load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencySupply(rs, dbKey);
    }

    @Override
    public void save(Connection con, CurrencySupply currencySupply) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_supply (id, current_supply, "
                + "current_reserve_per_unit_atm, height, latest, deleted) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, currencySupply.getCurrencyId());
            pstmt.setLong(++i, currencySupply.getCurrentSupply());
            pstmt.setLong(++i, currencySupply.getCurrentReservePerUnitATM());
            pstmt.setInt(++i, currencySupply.getHeight());
            pstmt.executeUpdate();
        }
    }

}
