/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
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

    @Inject
    public CurrencyFounderTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                DatabaseManager databaseManager,
                                Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("currency_founder", currencyFounderDbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public CurrencyFounder load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencyFounder(rs, dbKey);
    }

    @Override
    public void save(Connection con, CurrencyFounder currencyFounder) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_founder (currency_id, account_id, amount, height, latest, deleted) "
                + "KEY (currency_id, account_id, height) VALUES (?, ?, ?, ?, TRUE, FALSE)")
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
