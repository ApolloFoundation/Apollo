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
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class DbExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback, BeforeAllCallback {
    private static final Logger log = LoggerFactory.getLogger(DbExtension.class);
    private DbManipulator manipulator;
    private boolean staticInit = false;
    private FullTextSearchService ftl;
    private Map<String, List<String>> tableWithColumns;
    private Path indexDir;
    private Path dbDir;
    private LuceneFullTextSearchEngine luceneFullTextSearchEngine;

    public DbExtension(GenericContainer jdbcDatabaseContainer,
                       DbProperties dbProperties,
                       PropertiesHolder propertiesHolder,
                       String schemaScriptPath,
                       String dataScriptPath) {
        log.debug("JdbcUrl: {}", ((MariaDBContainer)jdbcDatabaseContainer).getJdbcUrl());
        log.debug("DockerDaemonInfo: {}", jdbcDatabaseContainer.getDockerDaemonInfo());
        log.debug("Username: {}", ((MariaDBContainer)jdbcDatabaseContainer).getUsername());
        dbProperties.setDbUsername(((MariaDBContainer)jdbcDatabaseContainer).getUsername());
        log.debug("DockerImageName: {}", jdbcDatabaseContainer.getDockerImageName());
        log.debug("DriverClassName: {}", ((MariaDBContainer)jdbcDatabaseContainer).getDriverClassName());
//        log.debug("05: {}", jdbcDatabaseContainer.getTestHostIpAddress());
        log.debug("ContainerId: {}", jdbcDatabaseContainer.getContainerId());
        log.debug("BoundPortNumbers: {}", jdbcDatabaseContainer.getBoundPortNumbers());
        log.debug("MappedPort: {}", jdbcDatabaseContainer.getMappedPort(3306));
        if (jdbcDatabaseContainer.getMappedPort(3306) != null) {
            dbProperties.setDatabasePort(jdbcDatabaseContainer.getMappedPort(3306));
        }
        log.debug("PortBindings: {}", jdbcDatabaseContainer.getPortBindings());
        log.debug("Host: {}", jdbcDatabaseContainer.getHost());
        log.debug("Host: {}", jdbcDatabaseContainer.getHost());
        dbProperties.setDatabaseHost(jdbcDatabaseContainer.getHost());
        dbProperties.setDatabaseName(((MariaDBContainer<?>) jdbcDatabaseContainer).getDatabaseName());

//        dbProperties.setDbUrl(((MariaDBContainer)jdbcDatabaseContainer).getJdbcUrl() + "?TC_DAEMON=true&TC_INITSCRIPT=file:src/test/resources/db/schema.sql");
//        dbProperties.setDbUrl("jdbc:tc:mariadb:10.4:///mysql?TC_DAEMON=true&TC_INITSCRIPT=file:src/test/resources/db/schema.sql");
//        dbProperties.setDbUrl("jdbc:mariadb://mariaDbService:3306/mysql");

        manipulator = new DbManipulator(dbProperties, propertiesHolder, dataScriptPath, schemaScriptPath);
//        this(dbProperties, null, null, null);
    }

    public DbExtension(GenericContainer jdbcDatabaseContainer, DbProperties dbProperties) {
        this(jdbcDatabaseContainer, dbProperties, null, null, null);
        log.debug("URL: {}", ((MariaDBContainer)jdbcDatabaseContainer).getJdbcUrl());
//        dbProperties.setDbUrl("jdbc:tc:mariadb:10.4:///mysql?TC_DAEMON=true&TC_INITSCRIPT=file:src/test/resources/db/schema.sql");
        log.debug("1: {}", jdbcDatabaseContainer.getDockerDaemonInfo());
        log.debug("2: {}", ((MariaDBContainer)jdbcDatabaseContainer).getUsername());
        log.debug("3: {}", jdbcDatabaseContainer.getDockerImageName());
    }

    public DbExtension(DbProperties dbProperties) {
        this(dbProperties, null, null, null);
    }

    public DbExtension(JdbcDatabaseContainer jdbcDatabaseContainer, DbProperties properties, String dataScriptPath, String schemaScriptPath) {
        manipulator = new DbManipulator(properties, null, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(DbProperties properties, String dataScriptPath, String schemaScriptPath) {
        manipulator = new DbManipulator(properties, null, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(Map<String, List<String>> tableWithColumns) {
        this();
        if (!tableWithColumns.isEmpty()) {
            this.tableWithColumns = tableWithColumns;
            //createFtl();
        }
    }

    public DbExtension(DbProperties dbProperties, PropertiesHolder propertiesHolder, String schemaScriptPath, String dataScriptPath) {
        manipulator = new DbManipulator(dbProperties, propertiesHolder, dataScriptPath, schemaScriptPath);
    }

    public DbExtension(Path dbDir, String dbName, String dataScript) {
        manipulator = new DbManipulator(DbTestData.getDbFileProperties(dbDir.resolve(dbName).toAbsolutePath().toString()), null, dataScript, null);
        this.dbDir = dbDir;
    }

    public DbExtension(JdbcDatabaseContainer jdbcDatabaseContainer, Path dbDir, String dbName, String dataScript) {
        manipulator = new DbManipulator(DbTestData.getDbFileProperties(dbDir.resolve(dbName).toAbsolutePath().toString()), null, dataScript, null);
        this.dbDir = dbDir;
    }

    public DbExtension() {
        manipulator = new DbManipulator(DbTestData.getInMemDbProps());
    }

    public FullTextSearchService getFtl() {
        return ftl;
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

/*    private void createFtl() {
        try {
            this.indexDir = Files.createTempDirectory("indexDir");
            this.luceneFullTextSearchEngine = new LuceneFullTextSearchEngine(mock(NtpTime.class), indexDir);
            this.ftl = new FullTextSearchServiceImpl(manipulator.getDatabaseManager(), luceneFullTextSearchEngine, tableWithColumns.keySet(), "PUBLIC");
        } catch (IOException e) {
            throw new RuntimeException("Unable to init ftl", e);
        }
    }*/

    private void initFtl() {
        ftl.init();
        tableWithColumns.forEach((table, columns) -> DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            try {
                ftl.createSearchIndex(con, table, String.join(",", columns));
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
        if (ftl != null) {
            initFtl();
        }
    }
}
