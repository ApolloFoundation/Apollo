/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag("slow")
public class DbUtilsTest extends DbContainerBaseTest {

    private static final TableData CURRENCY_TABLE_DATA = new TableData(
        0,
        "currency",
        "testdb",
        Arrays.asList("db_id", "id", "account_id", "name", "name_lower", "code", "description", "type", "initial_supply", "reserve_supply",
            "max_supply",
            "creation_height", "issuance_height", "min_reserve_per_unit_atm", "min_difficulty", "max_difficulty", "ruleset", "algorithm",
            "decimals", "height", "latest", "deleted"),
        Arrays.asList(
            Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.BIGINT, Types.BIGINT,
            Types.BIGINT,
            Types.INTEGER, Types.INTEGER, Types.BIGINT, Types.TINYINT, Types.TINYINT, Types.TINYINT, Types.TINYINT,
            Types.TINYINT, Types.INTEGER, Types.BIT, Types.BIT),
        Arrays.asList(5, 3, 6)
    );

    private static final TableData TWO_FACTOR_AUTH_TABLE_DATA = new TableData(
        -1,
        "two_factor_auth",
        "testdb",
        Arrays.asList("account", "secret", "confirmed"),
        Arrays.asList(Types.BIGINT, Types.LONGVARBINARY, Types.BIT),
        Collections.emptyList());

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, Map.of("currency", List.of("code", "name", "description")));

    @Tag("skip-fts-init")
    @Test
    public void testGetDbInfoForIndexedTable() throws SQLException {
        DataSource db = dbExtension.getDatabaseManager().getDataSource();
        try (Connection con = db.getConnection()) {
            TableData result = DbUtils.getTableData(con, "currency", "testdb");
            Assertions.assertEquals(CURRENCY_TABLE_DATA, result);
        }
    }

    @Tag("skip-fts-init")
    @Test
    public void testGetDbInfoForNonIndexedTable() throws SQLException {
        DataSource db = dbExtension.getDatabaseManager().getDataSource();
        try (Connection con = db.getConnection()) {
            TableData result = DbUtils.getTableData(con, "two_factor_auth", "testdb");
            Assertions.assertEquals(TWO_FACTOR_AUTH_TABLE_DATA, result);
        }
    }

}
