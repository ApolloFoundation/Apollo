/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author silaev-firstbridge on 1/8/2020
 */
class DataSourceWrapperTest {
    @Test
    void shouldConstructDataSourceWrapper() {
        //GIVEN
        final String dbType = "mariadb";
        final String dbName = "testdb";
        final String dbHost = "localhost";
        final Integer dbPort = 3306;
        final String user = "usr";
        final String pass = "pass";
        final String dbParams = "&TC_DAEMON=true&TC_REUSABLE=true";
        final DbProperties dbProperties = DbProperties.builder()
            .dbPassword(pass)
            .dbUsername(user)
            .databaseHost(dbHost)
            .databasePort(dbPort)
            .dbName(dbName)
            .dbParams(dbParams)
            .dbType(dbType)
            .build();

        //WHEN
        final DataSourceWrapper dataSourceWrapperActual = new DataSourceWrapper(dbProperties);

        //THEN
        assertNotNull(dataSourceWrapperActual);
        final String urlActual = dataSourceWrapperActual.getUrl();
        assertNotNull(urlActual);
        assertEquals(
            String.format(
                "jdbc:%s://%s:%d/%s?user=%s&password=%s%s",
                dbType, dbHost, dbPort, dbName, user, pass, dbParams
            ),
            urlActual.substring(0, urlActual.lastIndexOf("=") + 5)
        );

    }

}