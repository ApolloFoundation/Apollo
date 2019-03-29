/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import javax.sql.DataSource;

public class DbUtilsTest {
    private static final TableData CURRENCY_TABLE_DATA = new TableData(
            0,
            "currency",
            "PUBLIC",
            Arrays.asList("DB_ID", "ID", "ACCOUNT_ID", "NAME","NAME_LOWER", "CODE", "DESCRIPTION", "TYPE", "INITIAL_SUPPLY", "RESERVE_SUPPLY",
                    "MAX_SUPPLY",
                    "CREATION_HEIGHT", "ISSUANCE_HEIGHT", "MIN_RESERVE_PER_UNIT_ATM", "MIN_DIFFICULTY", "MAX_DIFFICULTY", "RULESET", "ALGORITHM",
                    "DECIMALS", "HEIGHT", "LATEST"),
            Arrays.asList(Types.BIGINT, Types.BIGINT
                    , Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.BIGINT, Types.BIGINT, Types.BIGINT,
                    Types.INTEGER, Types.INTEGER, Types.BIGINT, Types.TINYINT, Types.TINYINT, Types.TINYINT, Types.TINYINT, Types.TINYINT,
                    Types.INTEGER, Types.BOOLEAN),
            Arrays.asList(5, 3, 6)
    );

    private static final TableData TWO_FACTOR_AUTH_TABLE_DATA = new TableData(
            -1,
            "two_factor_auth",
            "PUBLIC",
            Arrays.asList("ACCOUNT", "SECRET", "CONFIRMED"),
            Arrays.asList(Types.BIGINT, Types.VARBINARY, Types.BOOLEAN),
            Collections.emptyList());

    @RegisterExtension
    DbExtension dbExtension = new DbExtension();
    @Test
    public void testGetDbInfoForIndexedTable() throws SQLException {
        DataSource db = dbExtension.getDatabaseManger().getDataSource();
        try (Connection con = db.getConnection()) {
            TableData result = DbUtils.getTableData(con, "currency", "PUBLIC");
            Assertions.assertEquals(CURRENCY_TABLE_DATA, result);
        }
    }

    @Test
    public void testGetDbInfoForNonIndexedTable() throws SQLException {
        DataSource db = dbExtension.getDatabaseManger().getDataSource();
        try (Connection con = db.getConnection()) {
            TableData result = DbUtils.getTableData(con, "two_factor_auth", "PUBLIC");
            Assertions.assertEquals(TWO_FACTOR_AUTH_TABLE_DATA, result);
        }
    }

}
