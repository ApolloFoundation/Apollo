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

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.enterprise.inject.spi.CDI;

/**
 * Compact and reorganize the ARS database.  The ARS application must not be
 * running.
 *
 * To run the database compact tool on Linux or Mac:
 *
 *   java -cp "classes:lib/*:conf" com.apollocurrency.aplwallet.apl.tools.CompactDatabase
 *
 * To run the database compact tool on Windows:
 *
 *   java -cp "classes;lib/*;conf" -Dapl.runtime.mode=desktop com.apollocurrency.aplwallet.apl.tools.CompactDatabase
 */
public class CompactDatabase {
    private static final Logger LOG = getLogger(CompactDatabase.class);

    // TODO: YL remove static instance later
    private PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

    public CompactDatabase() {        
    }
    
    /**
     */
    public void init() {
//TODO: Check and test this class
        DirProvider dirProvider = new DirProviderFactory()
                .getInstance(
                        RuntimeEnvironment.getInstance().isServiceMode(),
                        blockchainConfig.getChain().getChainId(),
                        Constants.APPLICATION_DIR_NAME,
                        null
                );
        AplCoreRuntime.getInstance().setup(RuntimeEnvironment.getInstance().getRuntimeMode(), dirProvider);
        AplCore core = new AplCore(blockchainConfig);
        core.init();

    }

    /**
     * Compact the database
     */
    public  int compactDatabase() {
        int exitCode = PosixExitCodes.OK.exitCode();
        //
        // Get the database URL
        //
        String dbPrefix = blockchainConfig.isTestnet() ? "apl.testDb" : "apl.db";
        String dbType = propertiesHolder.getStringProperty(dbPrefix + "Type");
        if (!"h2".equals(dbType)) {
            LOG.error("Database type must be 'h2'");
            return 1;
        }
        String dbUrl = propertiesHolder.getStringProperty(dbPrefix + "Url");
        if (dbUrl == null) {
            //TODO: check that runtime is inited
            String dbPath = AplCoreRuntime.getInstance().getDbDir().toAbsolutePath().toString();
            dbUrl = String.format("jdbc:%s:%s", dbType, dbPath);
        }
        String dbParams = propertiesHolder.getStringProperty(dbPrefix + "Params");
        dbUrl += ";" + dbParams;
        if (!dbUrl.contains("MV_STORE=")) {
            dbUrl += ";MV_STORE=FALSE";
        }
        String dbUsername = propertiesHolder.getStringProperty(dbPrefix + "Username", "sa");
        String dbPassword = propertiesHolder.getStringProperty(dbPrefix + "Password", "sa", true);
        //
        // Get the database path.  This is the third colon-separated operand and is
        // terminated by a semi-colon or by the end of the string.
        //
        int pos = dbUrl.indexOf(':');
        if (pos >= 0) {
            pos = dbUrl.indexOf(':', pos+1);
        }
        if (pos < 0) {
            LOG.error("Malformed database URL: " + dbUrl);
            return 1;
        }
        String dbDir;
        int startPos = pos + 1;
        int endPos = dbUrl.indexOf(';', startPos);
        if (endPos < 0) {
            dbDir = dbUrl.substring(startPos);
        } else {
            dbDir = dbUrl.substring(startPos, endPos);
        }
        //
        // Remove the optional 'file' operand
        //
        if (dbDir.startsWith("file:"))
            dbDir = dbDir.substring(5);
        //
        // Remove the database prefix from the end of the database path.  The path
        // separator can be either '/' or '\' (Windows will accept either separator
        // so we can't rely on the system property).
        //
        endPos = dbDir.lastIndexOf('\\');
        pos = dbDir.lastIndexOf('/');
        if (endPos >= 0) {
            if (pos >= 0) {
                endPos = Math.max(endPos, pos);
            }
        } else {
            endPos = pos;
        }
        if (endPos < 0) {
            LOG.error("Malformed database URL: " + dbUrl);
            return 1;
        }
        dbDir = dbDir.substring(0, endPos);
        LOG.info("Database directory is '" + dbDir + '"');
        //
        // Create our files
        //
        int phase = 0;
        File sqlFile = new File(dbDir, "backup.sql.gz");
        File dbFile = new File(dbDir, Constants.APPLICATION_DIR_NAME + "h2.db");
        if (!dbFile.exists()) {
            dbFile = new File(dbDir, Constants.APPLICATION_DIR_NAME + ".mv.db");
            if (!dbFile.exists()) {
                LOG.error("{} database not found", Constants.APPLICATION_DIR_NAME);
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
            try (Connection conn = getConnection(dbUrl, dbUsername, dbPassword);
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
            try (Connection conn = getConnection(dbUrl, dbUsername, dbPassword);
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
                    File newFile = new File(dbDir, Constants.APPLICATION_DIR_NAME + ".h2.db");
                    if (newFile.exists()) {
                        if (!newFile.delete()) {
                            LOG.error(String.format("Unable to delete '%s'", newFile.getPath()));
                        }
                    } else {
                        newFile = new File(dbDir, Constants.APPLICATION_DIR_NAME + ".mv.db");
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
        }
        catch (SQLException e) {
            LOG.error("Unable to connect to database", e);
        }
        return null;
    }
}
