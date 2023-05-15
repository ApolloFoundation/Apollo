/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
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

    @Inject
    public CurrencySupplyTable(DatabaseManager databaseManager,
                               Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("currency_supply", currencySupplyDbKeyFactory, null,
                databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public CurrencySupply load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencySupply(rs, dbKey);
    }

    public CurrencySupply get(long currencyId) {
        return get(currencySupplyDbKeyFactory.newKey(currencyId));
    }

    @Override
    public void save(Connection con, CurrencySupply currencySupply) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_supply (id, current_supply, "
                + "current_reserve_per_unit_atm, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), current_supply = VALUES(current_supply), "
                + "current_reserve_per_unit_atm = VALUES(current_reserve_per_unit_atm), height = VALUES(height),"
                + "latest = TRUE, deleted = FALSE")
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
