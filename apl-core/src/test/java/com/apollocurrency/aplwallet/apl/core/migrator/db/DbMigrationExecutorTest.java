/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.Chain;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainIdService;
import com.apollocurrency.aplwallet.apl.core.db.BasicDb;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.util.env.UserMode;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
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

    ChainIdService chainIdService = Mockito.mock(ChainIdService.class);
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    LegacyDbLocationsProvider legacyDbLocationsProvider = Mockito.mock(LegacyDbLocationsProvider.class);

//    H2DbInfoExtractor dbInfoExtractor = Mockito.mock(H2DbInfoExtractor.class);

    PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);

    private Path targetDbDir = createTempDir();
    private Path targetDbPath = targetDbDir.resolve(Constants.APPLICATION_DIR_NAME);
    BasicDb.DbProperties targetDbProperties = createDbProperties(targetDbPath);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(H2DbInfoExtractor.class)
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .addBeans(MockBean.of(legacyDbLocationsProvider, LegacyDbLocationsProvider.class))
            .addBeans(MockBean.of(Mockito.mock(Blockchain.class), BlockchainImpl.class))
            .addBeans(MockBean.of(Mockito.mock(BlockchainConfig.class), BlockchainConfig.class))
//            .addBeans(MockBean.of(dbInfoExtractor, DbInfoExtractor.class))
            .addBeans(MockBean.of(targetDbProperties, BasicDb.DbProperties.class))
            .bindResource("dbPassword", "sa")
            .bindResource("dbUsername", "sa")
            .build();


    private Path pathToDbForMigration;
    private DbManipulator manipulator;

    @BeforeEach
    void setUp() throws IOException {

        this.pathToDbForMigration = Files.createTempFile(Files.createTempDirectory("migrationDbDir"), "migrationDb-1", ".h2");
        manipulator = new DbManipulator(pathToDbForMigration, "sa", "sa");
        manipulator.init();
        Chain chain = new Chain();
        chain.setTestnet(true);
        Mockito.doReturn(chain).when(chainIdService).getActiveChain();
        Mockito.doReturn(true).when(propertiesHolder).getBooleanProperty("apl.migrator.db.deleteAfterMigration");
        Mockito.doReturn(100).when(propertiesHolder).getIntProperty("apl.batchCommitSize", Integer.MAX_VALUE);

        Db.init(targetDbProperties);
    }

    private BasicDb.DbProperties createDbProperties(Path p) {
        BasicDb.DbProperties dbProperties = new BasicDb.DbProperties()
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
        Files.deleteIfExists(pathToDbForMigration);
        manipulator.shutdown();
        FileUtils.deleteDirectory(targetDbDir.toFile());
    }


    @Test
    public void testDbMigration() throws IOException {
        DirProvider dirProvider = Mockito.mock(DirProvider.class);

        AplCoreRuntime.getInstance().setup(new UserMode(), dirProvider);
        Mockito.doReturn(targetDbDir).when(dirProvider).getDbDir();
        DbMigrationExecutor migrationExecutor = new DbMigrationExecutor(blockchainConfig, propertiesHolder);
        Mockito.doReturn(Arrays.asList(pathToDbForMigration)).when(legacyDbLocationsProvider).getDbLocations();
//        Mockito.doReturn(pathToDbForMigration).when(dbInfoExtractor).getPath(pathToDbForMigration.toAbsolutePath().toString());
//        Mockito.doReturn(Paths.get(targetDbPath.toAbsolutePath().toString() + ".h2.db")).when(dbInfoExtractor).getPath(targetDbPath.toAbsolutePath().toString());
//        Mockito.doReturn(10).when(dbInfoExtractor).getHeight(pathToDbForMigration.toAbsolutePath().toString());
//        Mockito.doReturn(10).when(dbInfoExtractor).getHeight(targetDbPath.toAbsolutePath().toString());
        migrationExecutor.performMigration(targetDbPath);
        OptionDAO optionDAO = new OptionDAO();
        String dbMigrated = optionDAO.get("dbMigrationRequired-0");
        Assertions.assertNotNull(dbMigrated);
        Assertions.assertEquals("false", dbMigrated);
        Assertions.assertFalse(Files.exists(pathToDbForMigration));
//        Assertions.assertEquals(new H2DbInfoExtractor());
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
