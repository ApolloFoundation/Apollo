/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.extension;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.LuceneFullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
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

//TODO Repair DbExtension to maintain the AfterEachCallback + nonstatic usage
@Slf4j
public class DbExtension implements BeforeEachCallback, /*AfterEachCallback,*/ AfterAllCallback, BeforeAllCallback {
    private DbManipulator manipulator;
    private boolean staticInit = false;
    private FullTextSearchService fullTextSearchService;
    private Map<String, List<String>> tableWithColumns;
    private Path indexDir;
    private LuceneFullTextSearchEngine luceneFullTextSearchEngine;

    public DbExtension(GenericContainer jdbcDatabaseContainer,
                       DbConfig dbConfig,
                       String schemaScriptPath,
                       String dataScriptPath) {
        assert dbConfig.getDbProperties() != null;

        log.trace("JdbcUrl: {}", ((MariaDBContainer) jdbcDatabaseContainer).getJdbcUrl());

        log.trace("Username: {}", ((MariaDBContainer) jdbcDatabaseContainer).getUsername());
        dbConfig.getDbProperties().setDbUsername(((MariaDBContainer) jdbcDatabaseContainer).getUsername());
        log.trace("User pass: {}", ((MariaDBContainer) jdbcDatabaseContainer).getPassword());
        dbConfig.getDbProperties().setDbPassword(((MariaDBContainer) jdbcDatabaseContainer).getPassword());
        log.trace("DriverClassName: {}", ((MariaDBContainer) jdbcDatabaseContainer).getDriverClassName());
        log.trace("MappedPort: {}", jdbcDatabaseContainer.getMappedPort(3306));
        if (jdbcDatabaseContainer.getMappedPort(3306) != null) {
            dbConfig.getDbProperties().setDatabasePort(jdbcDatabaseContainer.getMappedPort(3306));
        }
        log.trace("Host: {}", jdbcDatabaseContainer.getHost());
        dbConfig.getDbProperties().setDatabaseHost(jdbcDatabaseContainer.getHost());
        dbConfig.getDbProperties().setDbName(((MariaDBContainer<?>) jdbcDatabaseContainer).getDatabaseName());
        dbConfig.getDbProperties().setSystemDbUrl(dbConfig.getDbProperties().formatJdbcUrlString(true));

//        log.trace("DockerDaemonInfo: {}", jdbcDatabaseContainer.getDockerDaemonInfo());
        log.trace("DockerImageName: {}", jdbcDatabaseContainer.getDockerImageName());
        log.trace("ContainerId: {}", jdbcDatabaseContainer.getContainerId());
        log.trace("BoundPortNumbers: {}", jdbcDatabaseContainer.getBoundPortNumbers());
        log.trace("PortBindings: {}", jdbcDatabaseContainer.getPortBindings());

        this.manipulator = new DbManipulator(dbConfig, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer, DbConfig dbConfig) {
        this(jdbcDatabaseContainer, dbConfig, null, null);
    }

    public DbExtension(DbConfig dbConfig) {
        this.manipulator = new DbManipulator(dbConfig, null, null);
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer, Map<String, List<String>> tableWithColumns) {
        this(jdbcDatabaseContainer);
        if (!tableWithColumns.isEmpty()) {
            this.tableWithColumns = tableWithColumns;
            createFtl();
        }
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer) {
        this(jdbcDatabaseContainer, DbTestData.getDbFileProperties(jdbcDatabaseContainer),
            null, null);
    }

    public FullTextSearchService getFullTextSearchService() {
        return fullTextSearchService;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (context != null && context.getTags().contains("skip-fts-init")) {
            // skip init for some tests
            if (fullTextSearchService == null) {
                initFtl();
            }
        } else {
            if (fullTextSearchService != null) {
                initFtl();
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        shutdownDbAndDelete();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        manipulator.populate();
        if (fullTextSearchService != null) {
            initFtl();
        }
    }

    public void cleanAndPopulateDb() {
        TransactionalDataSource dataSource = manipulator.getDatabaseManager().getDataSource();
        if (dataSource.isInTransaction()) {
            dataSource.commit();
        }

        manipulator.populate();
    }

    public void shutdownDbAndDelete() throws IOException {
        manipulator.shutdown();
        if (fullTextSearchService != null) {
            fullTextSearchService.shutdown();
            FileUtils.deleteDirectory(indexDir.toFile());
            fullTextSearchService = null; // for next unit test run
        }
    }


    private void createFtl() {
        try {
            this.indexDir = Files.createTempDirectory("indexDir");
            this.luceneFullTextSearchEngine = new LuceneFullTextSearchEngine(mock(NtpTime.class), indexDir, null);
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
                    fullTextSearchService.createSearchIndex(con, table, String.join(",", columns));
                } else {
                    log.warn("NOTHING for fields... ");
                }
            } catch (SQLException e) {
                throw new RuntimeException("Unable to create index for table " + table, e);
            }
        }));
    }

    public LuceneFullTextSearchEngine getLuceneFullTextSearchEngine() {
        return luceneFullTextSearchEngine;
    }

    public DatabaseManager getDatabaseManager() {
        return manipulator.getDatabaseManager();
    }
}
