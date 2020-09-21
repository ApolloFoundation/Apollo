/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Testcontainers
@Tag("slow")
public class DbUtilsTest {
    @Container
    public static final GenericContainer mariaDBContainer = new MariaDBContainer("mariadb:10.5")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withExposedPorts(3306)
        .withLogConsumer(new Slf4jLogConsumer(log));

    private static final TableData CURRENCY_TABLE_DATA = new TableData(
        0,
        "currency",
        "PUBLIC",
        Arrays.asList("DB_ID", "ID", "ACCOUNT_ID", "NAME", "NAME_LOWER", "CODE", "DESCRIPTION", "TYPE", "INITIAL_SUPPLY", "RESERVE_SUPPLY",
            "MAX_SUPPLY",
            "CREATION_HEIGHT", "ISSUANCE_HEIGHT", "MIN_RESERVE_PER_UNIT_ATM", "MIN_DIFFICULTY", "MAX_DIFFICULTY", "RULESET", "ALGORITHM",
            "DECIMALS", "HEIGHT", "LATEST", "DELETED"),
        Arrays.asList(Types.BIGINT, Types.BIGINT
            , Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.BIGINT, Types.BIGINT, Types.BIGINT,
            Types.INTEGER, Types.INTEGER, Types.BIGINT, Types.TINYINT, Types.TINYINT, Types.TINYINT, Types.TINYINT, Types.TINYINT,
            Types.INTEGER, Types.BOOLEAN, Types.BOOLEAN),
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
    DbExtension dbExtension = new DbExtension(mariaDBContainer);

    @Test
    public void testGetDbInfoForIndexedTable() throws SQLException {
        DataSource db = dbExtension.getDatabaseManager().getDataSource();
        try (Connection con = db.getConnection()) {
            TableData result = DbUtils.getTableData(con, "currency", "PUBLIC");
            Assertions.assertEquals(CURRENCY_TABLE_DATA, result);
        }
    }

    @Test
    public void testGetDbInfoForNonIndexedTable() throws SQLException {
        DataSource db = dbExtension.getDatabaseManager().getDataSource();
        try (Connection con = db.getConnection()) {
            TableData result = DbUtils.getTableData(con, "two_factor_auth", "PUBLIC");
            Assertions.assertEquals(TWO_FACTOR_AUTH_TABLE_DATA, result);
        }
    }

}
