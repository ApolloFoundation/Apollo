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
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

public class CurrencyTable extends VersionedDeletableEntityDbTable<Currency> {

    public static final LongKeyFactory<Currency> currencyDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Currency currency) {
            if (currency.getDbKey() == null) {
                currency.setDbKey(super.newKey(currency.getCurrencyId()));
            }
            return currency.getDbKey();
        }
    };

    public CurrencyTable() {
        super("currency", currencyDbKeyFactory);
    }

    @Override
    public Currency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Currency(rs, dbKey);
    }

    @Override
    public void save(Connection con, Currency currency) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency (id, account_id, name, code, "
                + "description, type, initial_supply, reserve_supply, max_supply, creation_height, issuance_height, min_reserve_per_unit_atm, "
                + "min_difficulty, max_difficulty, ruleset, algorithm, decimals, height, latest, deleted) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, currency.getCurrencyId());
            pstmt.setLong(++i, currency.getAccountId());
            pstmt.setString(++i, currency.getName());
            pstmt.setString(++i, currency.getCode());
            pstmt.setString(++i, currency.getDescription());
            pstmt.setInt(++i, currency.getType());
            pstmt.setLong(++i, currency.getInitialSupply());
            pstmt.setLong(++i, currency.getReserveSupply());
            pstmt.setLong(++i, currency.getMaxSupply());
            pstmt.setInt(++i, currency.getCreationHeight());
            pstmt.setInt(++i, currency.getIssuanceHeight());
            pstmt.setLong(++i, currency.getMinReservePerUnitATM());
            pstmt.setByte(++i, (byte) currency.getMinDifficulty());
            pstmt.setByte(++i, (byte) currency.getMaxDifficulty());
            pstmt.setByte(++i, currency.getRuleset());
            pstmt.setByte(++i, currency.getAlgorithm());
            pstmt.setByte(++i, currency.getDecimals());
            pstmt.setInt(++i, currency.getHeight());
            pstmt.executeUpdate();
        }
    }

}
