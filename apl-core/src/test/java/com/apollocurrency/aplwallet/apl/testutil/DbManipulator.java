/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DbManipulator {
    private static final String DEFAULT_SCHEMA_SCRIPT_PATH = "db/schema.sql";
    private static final String DEFAULT_DATA_SCRIPT_PATH = "db/data.sql";
    private static final Logger logger = getLogger(DbManipulator.class);
    protected Path tempDbFile;
    protected DatabaseManager databaseManager;
    private ChainsConfigHolder chainCoinfig;
    private Chain chain;
    private final UUID chainId=UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5");    
    {
        chain = mock(Chain.class);
        when(chain.getChainId()).thenReturn(chainId);
        chainCoinfig = mock(ChainsConfigHolder.class);
        when(chainCoinfig.getActiveChain()).thenReturn(chain);        
    }
    private DbPopulator populator;

    public DbManipulator(Path dbFile, String dataScriptPath, String schemaScriptPath) {
        this(dbFile == null ? DbTestData.getInMemDbProps() : DbTestData.getDbFileProperties(dbFile.toAbsolutePath().toString()), null,  dataScriptPath, schemaScriptPath);
        this.tempDbFile = dbFile;
    }

    public DbManipulator(Path dbFile) {
        this(dbFile, null, null);
    }

    public DbManipulator(DbProperties dbProperties, PropertiesHolder propertiesHolder, String dataScriptPath, String schemaScriptPath) {
        Objects.requireNonNull(dbProperties, "dbProperties is NULL");
        PropertiesHolder propertiesHolderParam = propertiesHolder == null ? new PropertiesHolder() : propertiesHolder;
        this.databaseManager = new DatabaseManagerImpl(dbProperties, propertiesHolderParam, chainCoinfig);

        dataScriptPath = StringUtils.isBlank(dataScriptPath) ? DEFAULT_DATA_SCRIPT_PATH : dataScriptPath;
        schemaScriptPath = StringUtils.isBlank(schemaScriptPath) ? DEFAULT_SCHEMA_SCRIPT_PATH : schemaScriptPath;
        // sometimes it can be helpful to skip test data load
        if (propertiesHolder != null && !propertiesHolder.getBooleanProperty("apl.testData")) {
            // test data is not loaded
            logger.warn("-->> test data is not loaded from : {}", dataScriptPath);
            dataScriptPath = null;
        }
        this.populator = new DbPopulator(databaseManager.getDataSource(), schemaScriptPath, dataScriptPath);
    }

    public DbManipulator()  {
        this(null);
    }

    public DbManipulator(String dataScriptPath, String schemaScriptPath) {
        this(null, dataScriptPath, schemaScriptPath);
    }

    public void init() {
        populator.initDb();
    }

    public void shutdown() throws IOException {
        databaseManager.shutdown();
    }

    public Path getTempDbFile() {
        return tempDbFile;
    }

    public void populate() {
        populator.populateDb();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
