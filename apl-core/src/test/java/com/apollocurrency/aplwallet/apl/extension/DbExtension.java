/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.extension;

import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.LuceneFullTextSearchEngine;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class DbExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback, BeforeAllCallback {
    private static final Logger log = LoggerFactory.getLogger(DbExtension.class);
    private DbManipulator manipulator;
    private boolean staticInit = false;
    private FullTextSearchService fullTextSearchService;
    private Map<String, List<String>> tableWithColumns;
    private Path indexDir;
    private Path dbDir;
    private LuceneFullTextSearchEngine luceneFullTextSearchEngine;

    public DbExtension(GenericContainer jdbcDatabaseContainer,
                       DbProperties dbProperties,
                       PropertiesHolder propertiesHolder,
                       String schemaScriptPath,
                       String dataScriptPath) {
        log.trace("JdbcUrl: {}", ((MariaDBContainer)jdbcDatabaseContainer).getJdbcUrl());

        log.trace("Username: {}", ((MariaDBContainer)jdbcDatabaseContainer).getUsername());
        dbProperties.setDbUsername(((MariaDBContainer)jdbcDatabaseContainer).getUsername());
        log.trace("User pass: {}", ((MariaDBContainer)jdbcDatabaseContainer).getPassword());
        dbProperties.setDbPassword(((MariaDBContainer)jdbcDatabaseContainer).getPassword());
        log.trace("DriverClassName: {}", ((MariaDBContainer)jdbcDatabaseContainer).getDriverClassName());
        log.trace("MappedPort: {}", jdbcDatabaseContainer.getMappedPort(3306));
        if (jdbcDatabaseContainer.getMappedPort(3306) != null) {
            dbProperties.setDatabasePort(jdbcDatabaseContainer.getMappedPort(3306));
        }
        log.trace("Host: {}", jdbcDatabaseContainer.getHost());
        dbProperties.setDatabaseHost(jdbcDatabaseContainer.getHost());
        dbProperties.setDbName(((MariaDBContainer<?>) jdbcDatabaseContainer).getDatabaseName());

        log.trace("DockerDaemonInfo: {}", jdbcDatabaseContainer.getDockerDaemonInfo());
        log.trace("DockerImageName: {}", jdbcDatabaseContainer.getDockerImageName());
        log.trace("ContainerId: {}", jdbcDatabaseContainer.getContainerId());
        log.trace("BoundPortNumbers: {}", jdbcDatabaseContainer.getBoundPortNumbers());
        log.trace("PortBindings: {}", jdbcDatabaseContainer.getPortBindings());

        this.manipulator = new DbManipulator(dbProperties, propertiesHolder, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer, DbProperties dbProperties) {
        this(jdbcDatabaseContainer, dbProperties, null, null, null);
    }

    public DbExtension(DbProperties dbProperties) {
        this(dbProperties, null, null, null);
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer, DbProperties properties, String dataScriptPath, String schemaScriptPath) {
        this(jdbcDatabaseContainer, properties, null, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(DbProperties properties, String dataScriptPath, String schemaScriptPath) {
        this.manipulator = new DbManipulator(properties, null, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(Map<String, List<String>> tableWithColumns) {
        this();
        if (!tableWithColumns.isEmpty()) {
            this.tableWithColumns = tableWithColumns;
            createFtl();
        }
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer, Map<String, List<String>> tableWithColumns) {
        this(jdbcDatabaseContainer);
        if (!tableWithColumns.isEmpty()) {
            this.tableWithColumns = tableWithColumns;
            createFtl();
        }
    }

    public DbExtension(DbProperties dbProperties, PropertiesHolder propertiesHolder, String schemaScriptPath, String dataScriptPath) {
        this.manipulator = new DbManipulator(dbProperties, propertiesHolder, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer, Path dbDir, String dbName, String dataScript) {
        this(jdbcDatabaseContainer);
        this.manipulator = new DbManipulator(DbTestData.getInMemDbProps(), null, dataScript, null);
        this.dbDir = dbDir;
    }

    public DbExtension(Path dbDir, String dbName, String dataScript) {
        this.manipulator = new DbManipulator(DbTestData.getDbFileProperties(dbDir.resolve(dbName).toAbsolutePath().toString()), null, dataScript, null);
        this.dbDir = dbDir;
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer) {
        this(jdbcDatabaseContainer, DbTestData.getInMemDbProps(), null, null, null);
    }

    public DbExtension() {
        manipulator = new DbManipulator(DbTestData.getInMemDbProps());
    }

    public FullTextSearchService getFullTextSearchService() {
        return fullTextSearchService;
    }

    public LuceneFullTextSearchEngine getLuceneFullTextSearchEngine() {
        return luceneFullTextSearchEngine;
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
        if (fullTextSearchService != null) {
            fullTextSearchService.shutdown();
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
        if (!staticInit && fullTextSearchService != null) {
            initFtl();
        }
    }

    private void createFtl() {
        try {
            this.indexDir = Files.createTempDirectory("indexDir");
            this.luceneFullTextSearchEngine = new LuceneFullTextSearchEngine(mock(NtpTime.class), indexDir);
            Map<String, String> tableColumnsMap = new HashMap<>(5);
            Iterator<String> iterator = tableWithColumns.keySet().iterator();
            while (iterator.hasNext()) {
                String tableName = iterator.next();
                List<String> columns = tableWithColumns.get(tableName);
                String columnsJoined = String.join(",", columns);
                tableColumnsMap.put(tableName, columnsJoined);
            }
            this.fullTextSearchService = new FullTextSearchServiceImpl(manipulator.getDatabaseManager(),
                luceneFullTextSearchEngine, tableColumnsMap, "public");
        } catch (IOException e) {
            throw new RuntimeException("Unable to init ftl", e);
        }
    }

    private void initFtl() {
        fullTextSearchService.init();
        tableWithColumns.forEach((table, columns) -> DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            try {
                if (columns.size() > 0) {
                    fullTextSearchService.createSearchIndex(con, table,  String.join(",", columns));
                } else {
                    log.warn("NOTHING for fields... ");
                }
            } catch (SQLException e) {
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
        if (fullTextSearchService != null) {
            initFtl();
        }
    }
}
