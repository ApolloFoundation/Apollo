/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import com.apollocurrency.aplwallet.apl.FileUtils;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import javax.inject.Inject;

@EnableWeld
public class DbMigrationExecutorTest {

    private LegacyDbLocationsProvider legacyDbLocationsProvider = Mockito.mock(LegacyDbLocationsProvider.class);

    private FullTextSearchService fullTextSearchProvider = Mockito.mock(FullTextSearchService.class);
    private PropertiesHolder propertiesHolder = mockPropertiesHolder();
    private TemporaryFolder temporaryFolder = FileUtils.initTempFolder();


    private Path targetDbDir = createTempDir();
    private Path targetDbPath = targetDbDir.resolve(Constants.APPLICATION_DIR_NAME);
    private DbProperties targetDbProperties = DbTestData.getDbFileProperties(targetDbPath.toAbsolutePath().toString());
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(H2DbInfoExtractor.class, PropertyProducer.class,
            TransactionalDataSource.class, DatabaseManager.class)
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .addBeans(MockBean.of(Mockito.mock(Blockchain.class), BlockchainImpl.class))
            .addBeans(MockBean.of(targetDbProperties, DbProperties.class))
            .addBeans(MockBean.of(fullTextSearchProvider, FullTextSearchService.class))
            .build();
    @Inject
    private H2DbInfoExtractor h2DbInfoExtractor;
    private Path pathToDbForMigration;
    private DbManipulator manipulator;
    @Inject
    private DatabaseManager databaseManager;


    @BeforeEach
    void setUp() throws IOException {
        this.pathToDbForMigration = temporaryFolder.newFolder().toPath().resolve(
            "migrationDb-1");
        manipulator = new DbManipulator(pathToDbForMigration, "sa", "sa");
        manipulator.init();
        manipulator.populate();
        manipulator.shutdown();
    }

    PropertiesHolder mockPropertiesHolder() {
        Properties properties = new Properties();
        properties.put("apl.migrator.db.deleteAfterMigration", true);
        properties.put("apl.batchCommitSize", 100);
        properties.put("apl.dbPassword", "sa");
        properties.put("apl.dbUsername", "sa");
        properties.put("apl.dbParams", "DB_CLOSE_ON_EXIT=FALSE;MVCC=TRUE;MV_STORE=FALSE;");
        PropertiesHolder ph = new PropertiesHolder();
        ph.init(properties);
        return ph;
    }



    @AfterEach
    void tearDown() throws Exception {
//        TimeUnit.SECONDS.sleep(5);
        databaseManager.shutdown();
        temporaryFolder.delete();
    }



    @Test
    public void testDbMigrationWhenNoDbsFound() throws IOException {
        DbMigrationExecutor migrationExecutor = new DbMigrationExecutor(propertiesHolder, legacyDbLocationsProvider, h2DbInfoExtractor, databaseManager, fullTextSearchProvider);
        Mockito.doReturn(Collections.emptyList()).when(legacyDbLocationsProvider).getDbLocations();
        migrationExecutor.performMigration(targetDbPath);
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        String dbMigrated = optionDAO.get("dbMigrationRequired-0");
        Assertions.assertNotNull(dbMigrated);
        Assertions.assertEquals("false", dbMigrated);
        Assertions.assertTrue(Files.exists(h2DbInfoExtractor.getPath(pathToDbForMigration.toAbsolutePath().toString())));
        databaseManager.shutdown();
        int migratedHeight = h2DbInfoExtractor
                .getHeight(targetDbPath.toAbsolutePath().toString());
        Assertions.assertEquals(0, migratedHeight);
    }

    @Test
    public void testDbMigration() throws IOException {
        DbMigrationExecutor migrationExecutor = new DbMigrationExecutor(propertiesHolder, legacyDbLocationsProvider, h2DbInfoExtractor, databaseManager, fullTextSearchProvider);
        Mockito.doReturn(Arrays.asList(pathToDbForMigration)).when(legacyDbLocationsProvider).getDbLocations();
        migrationExecutor.performMigration(targetDbPath);
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        String dbMigrated = optionDAO.get("dbMigrationRequired-0");
        Assertions.assertNotNull(dbMigrated);
        Assertions.assertEquals("false", dbMigrated);
        Assertions.assertFalse(Files.exists(h2DbInfoExtractor.getPath(pathToDbForMigration.toAbsolutePath().toString())));
        databaseManager.shutdown();
        int migratedHeight = h2DbInfoExtractor.getHeight(targetDbPath.toAbsolutePath().toString());
        Assertions.assertEquals(104671, migratedHeight);
    }

    private Path createTempDir() {
        try {
            return temporaryFolder.newFolder("target").toPath();
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to create temp dir");
        }
    }
}
