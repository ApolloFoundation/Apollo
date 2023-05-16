/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableMarkerInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
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
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;

@Singleton
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
public class CurrencyTable extends VersionedDeletableEntityDbTable<Currency> implements SearchableTableMarkerInterface<Currency> {

    public static final String TABLE_NAME = "currency";
    public static final String FULL_TEXT_SEARCH_COLUMNS = "code,name,description";

    public static final LongKeyFactory<Currency> currencyDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Currency currency) {
            if (currency.getDbKey() == null) {
                currency.setDbKey(super.newKey(currency.getId()));
            }
            return currency.getDbKey();
        }
    };

    @Inject
    public CurrencyTable(DatabaseManager databaseManager,
                         Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME, currencyDbKeyFactory, FULL_TEXT_SEARCH_COLUMNS,
                databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public Currency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Currency(rs, dbKey);
    }

    public Currency get(long id) {
        return get(currencyDbKeyFactory.newKey(id));
    }

    @Override
    public void save(Connection con, Currency currency) throws SQLException {
        Objects.requireNonNull(currency);
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency (id, account_id, `name`, name_lower, code, "
                    + "description, `type`, initial_supply, reserve_supply, max_supply, creation_height, issuance_height, min_reserve_per_unit_atm, "
                    + "min_difficulty, max_difficulty, ruleset, algorithm, decimals, height, latest, deleted) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) "
                    + "ON DUPLICATE KEY UPDATE id = VALUES(id), account_id = VALUES(account_id), `name` = VALUES(`name`), name_lower = VALUES(name_lower), "
                    + "code = VALUES(code), description = VALUES(description), `type` = VALUES(`type`), initial_supply = VALUES(initial_supply), "
                    + "reserve_supply = VALUES(reserve_supply), max_supply = VALUES(max_supply), creation_height = VALUES(creation_height), "
                    + "issuance_height = VALUES(issuance_height), min_reserve_per_unit_atm = VALUES(min_reserve_per_unit_atm), "
                    + "min_difficulty = VALUES(min_difficulty), max_difficulty = VALUES(max_difficulty), ruleset = VALUES(ruleset), "
                    + "algorithm = VALUES(algorithm), decimals = VALUES(decimals), height = VALUES(height), latest = TRUE, deleted = FALSE",
                Statement.RETURN_GENERATED_KEYS)
        ) {
            final String name = currency.getName();
            Objects.requireNonNull(name);

            int i = 0;
            pstmt.setLong(++i, currency.getId());
            pstmt.setLong(++i, currency.getAccountId());
            pstmt.setString(++i, name);
            pstmt.setString(++i, name.toLowerCase(Locale.US));
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
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    currency.setDbId(rs.getLong(1));
                }
            }
        }
    }

}
