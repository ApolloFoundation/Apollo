/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.extension;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchServiceImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.LuceneFullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback, BeforeAllCallback {
    private static final Logger log = LoggerFactory.getLogger(DbExtension.class);
    private DbManipulator manipulator;
    private boolean staticInit = false;
    private FullTextSearchService ftl;
    private Map<String, List<String>> tableWithColumns;
    private Path indexDir;
    private Path dbDir;
    private LuceneFullTextSearchEngine luceneFullTextSearchEngine;

    public DbExtension(DbProperties dbProperties) {
        this(dbProperties, null, null, null);
    }

    public DbExtension(DbProperties properties, String dataScriptPath, String schemaScriptPath) {
        manipulator = new DbManipulator(properties, null, dataScriptPath, schemaScriptPath);
    }



    public DbExtension(Map<String, List<String>> tableWithColumns) {
        this();
        if (!tableWithColumns.isEmpty()) {
            this.tableWithColumns = tableWithColumns;
            createFtl();
        }
    }

    public FullTextSearchService getFtl() {
        return ftl;
    }

    public LuceneFullTextSearchEngine getLuceneFullTextSearchEngine() {
        return luceneFullTextSearchEngine;
    }

    public DbExtension(DbProperties dbProperties, PropertiesHolder propertiesHolder, String schemaScriptPath, String dataScriptPath) {
        manipulator = new DbManipulator(dbProperties, propertiesHolder, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(Path dbDir, String dbName, String dataScript) {
        manipulator = new DbManipulator(DbTestData.getDbFileProperties(dbDir.resolve(dbName).toAbsolutePath().toString()), null, dataScript, null);
        this.dbDir = dbDir;
    }

    public DbExtension() {
        manipulator = new DbManipulator(DbTestData.getInMemDbProps());
    }

    public DatabaseManager getDatabaseManager() {
        return manipulator.getDatabaseManager();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!staticInit) {
            shutdownDbAndDelete();
        }
    }

    private void shutdownDbAndDelete() throws IOException {
        manipulator.shutdown();
        if (ftl != null) {
            ftl.shutdown();
            FileUtils.deleteDirectory(indexDir.toFile());
        }
        if (dbDir != null) {
            FileUtils.deleteDirectory(dbDir.toFile());
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (!staticInit) {
            manipulator.init();
        }
        manipulator.populate();
        if (!staticInit && ftl != null) {
            initFtl();
        }
    }

    private void createFtl() {
        try {
            this.indexDir = Files.createTempDirectory("indexDir");
            this.luceneFullTextSearchEngine = new LuceneFullTextSearchEngine(mock(NtpTime.class), indexDir);
            this.ftl = new FullTextSearchServiceImpl(manipulator.getDatabaseManager(), luceneFullTextSearchEngine, tableWithColumns.keySet(), "PUBLIC");
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to init ftl", e);
        }
    }

    private void initFtl() {
        ftl.init();
        tableWithColumns.forEach((table, columns) -> DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            try {
                ftl.createSearchIndex(con, table, String.join(",", columns));
            }
            catch (SQLException e) {
                throw new RuntimeException("Unable to create index for table " + table, e);
            }
        }));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        shutdownDbAndDelete();
        staticInit = false;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        staticInit = true;
        manipulator.init();
        if (ftl != null) {
            initFtl();
        }
    }
}
