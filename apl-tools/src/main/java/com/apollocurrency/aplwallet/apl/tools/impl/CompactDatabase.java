/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Compact and reorganize the ARS database.  The ARS application must not be
 * running.
 * <p>
 * To run the database compact tool on Linux or Mac:
 * <p>
 * java -cp "classes:lib/*:conf" com.apollocurrency.aplwallet.apl.tools.CompactDatabase
 * <p>
 * To run the database compact tool on Windows:
 * <p>
 * java -cp "classes;lib/*;conf" -Dapl.runtime.mode=desktop com.apollocurrency.aplwallet.apl.tools.CompactDatabase
 */
public class CompactDatabase {
    private static final Logger LOG = getLogger(CompactDatabase.class);

    // TODO: YL remove static instance later
    private final PropertiesHolder propertiesHolder;
    private final DirProvider dirProvider;

    //TODO: Check and test this class
    public CompactDatabase(PropertiesHolder propertiesHolder, DirProvider dirProvider) {
        this.propertiesHolder = propertiesHolder;
        this.dirProvider = dirProvider;
    }

    /**
     * Compact the database
     *
     * @return exit code indication error or success
     */
    public int compactDatabase() {
        int exitCode = PosixExitCodes.OK.exitCode();
        //
        // Get the database URL
        //
        ConfigDirProvider configDirProvider = ConfigDirProviderFactory.getConfigDirProvider();
        ChainsConfigHolder chainsConfigHolder = new ChainsConfigHolder();
        Map<UUID, Chain> loadedChains = new ChainsConfigLoader(configDirProvider, false).load();
        DbProperties dbProperties = new DbConfig(propertiesHolder, chainsConfigHolder).getDbConfig();
        chainsConfigHolder.setChains(loadedChains);
        if (!"h2".equals(dbProperties.getDbType())) {
            LOG.error("Database type must be 'h2'");
            return 1;
        }

        //
        // Create our files
        //
        int phase = 0;
        //TODO: this SQL script is lost. Dvelop new one and place in resources,
        // read it from resources
        File sqlFile = new File(dbProperties.getDbDir(), "backup.sql.gz");
        File dbFile = new File(dbProperties.getDbDir(), dbProperties.getDbFileName() + DbProperties.DB_EXTENSION_WITH_DOT);
        if (!dbFile.exists()) {
            dbFile = new File(dbProperties.getDbDir(), Constants.APPLICATION_DIR_NAME + ".mv.db");
            if (!dbFile.exists()) {
                LOG.error("{} database not found", dbProperties.getDbDir());
                return 1;
            }
        }
        File oldFile = new File(dbFile.getPath() + ".bak");
        try {
            //
            // Create the SQL script
            //
            LOG.info("Creating the SQL script");
            if (sqlFile.exists()) {
                if (!sqlFile.delete()) {
                    throw new IOException(String.format("Unable to delete '%s'", sqlFile.getPath()));
                }
            }
            try (Connection conn = getConnection(dbProperties.getDbUrl(), dbProperties.getDbUsername(), dbProperties.getDbPassword());
                 Statement s = conn.createStatement()) {
                s.execute("SCRIPT TO '" + sqlFile.getPath() + "' COMPRESSION GZIP CHARSET 'UTF-8'");
            }
            //
            // Create the new database
            //
            LOG.info("Creating the new database");
            if (!dbFile.renameTo(oldFile)) {
                throw new IOException(String.format("Unable to rename '%s' to '%s'",
                    dbFile.getPath(), oldFile.getPath()));
            }
            phase = 1;
            try (Connection conn = getConnection(dbProperties.getDbUrl(), dbProperties.getDbUsername(), dbProperties.getDbPassword());
                 Statement s = conn.createStatement()) {
                s.execute("RUNSCRIPT FROM '" + sqlFile.getPath() + "' COMPRESSION GZIP CHARSET 'UTF-8'");
                s.execute("ANALYZE");
            }
            //
            // New database has been created
            //
            phase = 2;
            LOG.info("Database successfully compacted");
        } catch (Throwable exc) {
            LOG.error("Unable to compact the database", exc);
            exitCode = PosixExitCodes.EX_OSFILE.exitCode();
        } finally {
            switch (phase) {
                case 0:
                    //
                    // We failed while creating the SQL file
                    //
                    if (sqlFile.exists()) {
                        if (!sqlFile.delete()) {
                            LOG.error(String.format("Unable to delete '%s'", sqlFile.getPath()));
                        }
                    }
                    break;
                case 1:
                    //
                    // We failed while creating the new database
                    //
                    File newFile = new File(dbProperties.getDbDir(), Constants.APPLICATION_DIR_NAME + DbProperties.DB_EXTENSION_WITH_DOT);
                    if (newFile.exists()) {
                        if (!newFile.delete()) {
                            LOG.error(String.format("Unable to delete '%s'", newFile.getPath()));
                        }
                    } else {
                        newFile = new File(dbProperties.getDbDir(), Constants.APPLICATION_DIR_NAME + ".mv.db");
                        if (newFile.exists()) {
                            if (!newFile.delete()) {
                                LOG.error(String.format("Unable to delete '%s'", newFile.getPath()));
                            }
                        }
                    }
                    if (!oldFile.renameTo(dbFile)) {
                        LOG.error(String.format("Unable to rename '%s' to '%s'",
                            oldFile.getPath(), dbFile.getPath()));
                    }
                    break;
                case 2:
                    //
                    // New database created
                    //
                    if (!sqlFile.delete()) {
                        LOG.error(String.format("Unable to delete '%s'", sqlFile.getPath()));
                    }
                    if (!oldFile.delete()) {
                        LOG.error(String.format("Unable to delete '%s'", oldFile.getPath()));
                    }
                    break;
            }
        }
        return exitCode;
    }

    public Connection getConnection(String url, String user, String password) {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            LOG.error("Unable to connect to database", e);
        }
        return null;
    }
}
