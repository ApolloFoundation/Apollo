/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import java.util.List;

public class DatabaseAdministratorFactoryImpl implements DatabaseAdministratorFactory {
    private static final String JDBC_PREFIX = "jdbc";
    private static final String H2_DB_TYPE = "h2";
    private static final String H2_DB_PREFIX = prefix(H2_DB_TYPE);
    private static final String MARIA_DB_TYPE = "mariadb";
    private static final String MARIA_DB_PREFIX = prefix(MARIA_DB_TYPE);
    private static final List<String> SUPPORTED_DB_TYPES = List.of(H2_DB_TYPE, MARIA_DB_TYPE);
    private static final List<String> SUPPORTED_DB_PREFIXES = List.of(H2_DB_PREFIX, MARIA_DB_PREFIX);

    private final DirProvider dirProvider;

    public DatabaseAdministratorFactoryImpl(DirProvider dirProvider) {
        this.dirProvider = dirProvider;
    }

    @Override
    public DatabaseAdministrator createDbAdmin(DbProperties dbProperties) {
        String dbUrl = dbProperties.getDbUrl();
        String dbType = analyzeDbType(dbProperties, dbUrl);
        switch (dbType) {
            case H2_DB_TYPE:
                return new H2DatabaseAdministrator(dbProperties);
            case MARIA_DB_TYPE:
                return new MariadbDatabaseAdministrator(dirProvider, dbProperties);
            default:
                throw new RuntimeException("Fatal error! Unsupported dbType " + dbType + ", should never happen here, possibly previous verification code was changed");
        }
    }


    private String analyzeDbType(DbProperties dbProperties, String dbUrl) {
        String dbType;
        if (StringUtils.isNotBlank(dbUrl)) {
            dbType = findSupportedDbTypeInUrl(dbUrl);
        } else {
            dbType = dbProperties.getDbType();
        }
        verifyDbTypeSupported(dbType);
        return dbType;
    }

    private String findSupportedDbTypeInUrl(String dbUrl) {
        for (String supportedDbPrefix : SUPPORTED_DB_PREFIXES) {
            if (dbUrl.startsWith(supportedDbPrefix)) {
                return prefixToType(supportedDbPrefix);
            }
        }
        throw new IllegalArgumentException("Unsupported custom jdbc url: '" + dbUrl + "', unable to determine supported db type from the range: " + SUPPORTED_DB_PREFIXES);
    }

    private void verifyDbTypeSupported(String dbType) {
        for (String supportedDbType : SUPPORTED_DB_TYPES) {
            if (supportedDbType.equals(dbType)) {
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported db type: '" + dbType + "', valid db types list: " + SUPPORTED_DB_TYPES);
    }


    private static String prefix(String type) {
        return JDBC_PREFIX + ":" + type + ":";
    }

    private static String prefixToType(String prefix) {
        return prefix.substring(prefix.indexOf(":"), prefix.lastIndexOf(":"));
    }
}
