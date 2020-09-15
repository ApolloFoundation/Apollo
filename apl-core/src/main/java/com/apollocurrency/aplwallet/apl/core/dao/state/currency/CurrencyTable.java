/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

@Singleton
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
public class CurrencyTable extends VersionedDeletableEntityDbTable<Currency> implements SearchableTableInterface<Currency> {

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
    public CurrencyTable(DerivedTablesRegistry derivedDbTablesRegistry,
                         DatabaseManager databaseManager,
                         FullTextConfig fullTextConfig,
                         Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("currency", currencyDbKeyFactory, "code,name,description",
            derivedDbTablesRegistry, databaseManager, fullTextConfig, deleteOnTrimDataEvent);
    }

    @Override
    public Currency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Currency(rs, dbKey);
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
                + "algorithm = VALUES(algorithm), decimals = VALUES(decimals), height = VALUES(height), latest = TRUE, deleted = FALSE")
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
        }
    }

    @Override
    public final DbIterator<Currency> search(String query, DbClause dbClause, int from, int to) {
        return search(query, dbClause, from, to, " ORDER BY ft.score DESC ");
    }

    @Override
    public final DbIterator<Currency> search(String query, DbClause dbClause, int from, int to, String sort) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement("SELECT " + table + ".*, ft.score FROM " + table +
                ", ftl_search('PUBLIC', '" + table + "', ?, 2147483647, 0) ft "
                + " WHERE " + table + ".db_id = ft.keys[1] "
                + (multiversion ? " AND " + table + ".latest = TRUE " : " ")
                + " AND " + dbClause.getClause() + sort
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setString(++i, query);
            i = dbClause.set(pstmt, ++i);
            i = DbUtils.setLimits(i, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }


}
