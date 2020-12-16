/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;


public class DbManipulator {
    private static final String DEFAULT_SCHEMA_SCRIPT_PATH = "db/schema.sql";
    private static final String DEFAULT_DATA_SCRIPT_PATH = "db/data.sql";
    private static final Logger logger = getLogger(DbManipulator.class);
    protected DatabaseManager databaseManager;

    private DbPopulator populator;


    public DbManipulator(DbProperties dbProperties, PropertiesHolder propertiesHolder, String dataScriptPath, String schemaScriptPath) {
        Objects.requireNonNull(dbProperties, "dbProperties is NULL");
        PropertiesHolder propertiesHolderParam = propertiesHolder == null ? new PropertiesHolder() : propertiesHolder;
        this.databaseManager = new DatabaseManagerImpl(dbProperties, propertiesHolderParam, new JdbiHandleFactory());

        dataScriptPath = StringUtils.isBlank(dataScriptPath) ? DEFAULT_DATA_SCRIPT_PATH : dataScriptPath;
        schemaScriptPath = StringUtils.isBlank(schemaScriptPath) ? DEFAULT_SCHEMA_SCRIPT_PATH : schemaScriptPath;
        // sometimes it can be helpful to skip test data load
        this.populator = new DbPopulator(schemaScriptPath, dataScriptPath);
    }

    public void init() {
        populator.initDb(databaseManager.getDataSource());
    }

    public void shutdown() {
        databaseManager.shutdown();
    }

    public void populate() {
        populator.populateDb(databaseManager.getDataSource());
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
