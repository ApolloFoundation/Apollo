/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.DbProperties;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableWeld
public class DbMigrationExecutorTest {

    private LegacyDbLocationsProvider legacyDbLocationsProvider = Mockito.mock(LegacyDbLocationsProvider.class);

    private BlockchainConfig blockchainConfig = mockBlockchainConfig();
    private PropertiesHolder propertiesHolder = mockPropertiesHolder();

    private Path targetDbDir = createTempDir();
    private Path targetDbPath = targetDbDir.resolve(Constants.APPLICATION_DIR_NAME);
    private DbProperties targetDbProperties = createDbProperties(targetDbPath);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(H2DbInfoExtractor.class, PropertyProducer.class)
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(legacyDbLocationsProvider, LegacyDbLocationsProvider.class))
            .addBeans(MockBean.of(Mockito.mock(Blockchain.class), BlockchainImpl.class))
            .addBeans(MockBean.of(targetDbProperties, DbProperties.class))
            .build();

    private Path pathToDbForMigration;
    private DbManipulator manipulator;

    @BeforeEach
    void setUp() throws IOException {
        this.pathToDbForMigration = Files.createTempFile(Files.createTempDirectory("migrationDbDir"), "migrationDb-1", ".h2");
        manipulator = new DbManipulator(pathToDbForMigration, "sa", "sa");
        manipulator.init();
        manipulator.populate();
        Db.init(targetDbProperties);
    }

    PropertiesHolder mockPropertiesHolder() {
        Properties properties = new Properties();
        properties.put("apl.migrator.db.deleteAfterMigration", true);
        properties.put("apl.batchCommitSize", 100);
        properties.put("apl.testDbPassword", "sa");
        properties.put("apl.testDbUsername", "sa");
        return new PropertiesHolder(properties);
    }

    BlockchainConfig mockBlockchainConfig() {
        BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
        Mockito.doReturn(true).when(blockchainConfig).isTestnet();
        return blockchainConfig;
    }
    private DbProperties createDbProperties(Path p) {
        DbProperties dbProperties = new DbProperties()
                .dbDir(p.getParent().toAbsolutePath().toString())
                .dbFileName(p.getFileName().toString())
                .dbPassword("sa")
                .dbUsername("sa")
                .dbType("h2")
                .loginTimeout(1000 * 30)
                .maxMemoryRows(100000)
                .dbParams("")
                .maxConnections(100)
                .maxCacheSize(0);

        return dbProperties;
    }


    @AfterEach
    void tearDown() throws Exception {
//        Db.shutdown();
        Files.deleteIfExists(pathToDbForMigration);
        manipulator.shutdown();
        FileUtils.deleteDirectory(targetDbDir.toFile());
    }


    @Test
    public void testDbMigrationWhenNoDbsFound() throws IOException {
        DbMigrationExecutor migrationExecutor = new DbMigrationExecutor(blockchainConfig, propertiesHolder);
        Mockito.doReturn(Collections.emptyList()).when(legacyDbLocationsProvider).getDbLocations();
        migrationExecutor.performMigration(targetDbPath);
        OptionDAO optionDAO = new OptionDAO();
        String dbMigrated = optionDAO.get("dbMigrationRequired-0");
        Assertions.assertNotNull(dbMigrated);
        Assertions.assertEquals("false", dbMigrated);
        Assertions.assertTrue(Files.exists(pathToDbForMigration));
        int migratedHeight = new H2DbInfoExtractor("sa", "sa")
                .getHeight(targetDbPath.toAbsolutePath().toString());
        Assertions.assertEquals(0, migratedHeight);
    }

    @Test
    public void testDbMigration() throws IOException {
        DbMigrationExecutor migrationExecutor = new DbMigrationExecutor(blockchainConfig, propertiesHolder);
        Mockito.doReturn(Arrays.asList(pathToDbForMigration)).when(legacyDbLocationsProvider).getDbLocations();
        migrationExecutor.performMigration(targetDbPath);
        OptionDAO optionDAO = new OptionDAO();
        String dbMigrated = optionDAO.get("dbMigrationRequired-0");
        Assertions.assertNotNull(dbMigrated);
        Assertions.assertEquals("false", dbMigrated);
        Assertions.assertFalse(Files.exists(pathToDbForMigration));
        int migratedHeight = new H2DbInfoExtractor("sa", "sa").getHeight(targetDbPath.toAbsolutePath().toString());
        Assertions.assertEquals(104671, migratedHeight);
    }

    private Path createTempDir() {
        try {
            return Files.createTempDirectory("targetDbDir");
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to create temp dir");
        }
    }
}
