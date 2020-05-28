/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author silaev-firstbridge on 1/8/2020
 */
class DataSourceWrapperTest {
    @Test
    void shouldConstructDataSourceWrapper() {
        //GIVEN
        final String dbType = "h2";
        final String dbFileName = "2f2b61";
        final String dbDir = "/usr/db";
        final String user = "usr";
        final String pass = "pass";
        final String dbParams = "AUTO_SERVER=TRUE;TRACE_LEVEL_FILE=1";
        final DbProperties dbProperties = new DbProperties()
            .dbPassword(pass)
            .dbUsername(user)
            .dbDir(dbDir)
            .dbFileName(dbFileName)
            .dbParams(dbParams)
            .dbType(dbType);

        //WHEN
        final DataSourceWrapper dataSourceWrapperActual = new DataSourceWrapper(dbProperties);

        //THEN
        assertNotNull(dataSourceWrapperActual);
        final String urlActual = dataSourceWrapperActual.getUrl();
        assertNotNull(urlActual);
        assertEquals(
            String.format(
                "jdbc:%s:file:%s/%s;%s",
                dbType, dbDir, dbFileName, dbParams + ";MV_STORE=TRUE;CACHE_SIZE="
            ),
            urlActual.substring(0, urlActual.lastIndexOf("=") + 1)
        );

    }

    @ParameterizedTest(name = "{index}: dbParams: {0}")
    @ValueSource(strings = {"MVCC=TRUE", "MV_STORE=FALSE", "MVCC=TRUE;MV_STORE=FALSE"})
    void shouldNotConstructDataSourceWrapperBecauseOfIncorrectDbUrl(String dbParams) {
        //GIVEN
        final String url = "jdbc:h2:file:C:/db/2f2b61/apl-blockchain;AUTO_SERVER=TRUE;" + dbParams;
        final DbProperties dbProperties = new DbProperties().dbUrl(url);

        //WHEN
        final Executable executable = () -> new DataSourceWrapper(dbProperties);

        //THEN
        assertThrows(IllegalArgumentException.class, executable);
    }

    @ParameterizedTest(name = "{index}: dbParams: {0}")
    @ValueSource(strings = {"MVCC=TRUE", "MV_STORE=FALSE", "MVCC=TRUE;MV_STORE=FALSE"})
    void shouldNotConstructDataSourceWrapperBecauseOfIncorrectDbParams(String dbParams) {
        //GIVEN
        final DbProperties dbProperties = new DbProperties().dbParams(dbParams);

        //WHEN
        final Executable executable = () -> new DataSourceWrapper(dbProperties);

        //THEN
        assertThrows(IllegalArgumentException.class, executable);
    }
}