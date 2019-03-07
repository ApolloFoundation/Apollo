package com.apollocurrency.aplwallet.apl.core.db.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;

import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OptionDAOTest {

    private static String BASE_SUB_DIR = "unit-test-db";

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class)
            .build();

    private static Path pathToDb;
    private static PropertiesHolder propertiesHolder;
    private static DbProperties baseDbProperties;
    private static DatabaseManager databaseManager;
    private static OptionDAO optionDAO;

    @BeforeAll
    static void setUpAll() {
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);
        String workingDir = System.getProperty("user.dir");
        pathToDb = FileSystems.getDefault().getPath(workingDir + File.separator  + BASE_SUB_DIR);
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                configDirProvider,
                false,
                "./" + BASE_SUB_DIR,
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        propertiesHolder.init(propertiesLoader.load());
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        baseDbProperties = dbConfig.getDbConfig();
        databaseManager = new DatabaseManager(baseDbProperties, propertiesHolder);
        optionDAO = new OptionDAO(databaseManager);
    }

    @AfterAll
    static void stopAll() {
        databaseManager.shutdown();
    }

    @Test
    void get() {
        String value = optionDAO.get("unknown_key_1");
        assertNull(value);
    }

    @Test
    void getWithDataSource() {
        String value = optionDAO.get("unknown_key_1", databaseManager.getDataSource());
        assertNull(value);
    }

    @Test
    void set() {
        String unknown_key = "unknown_key_2";
        boolean isInserted = optionDAO.set(unknown_key, "unknown_value");
        assertTrue(isInserted);
        String value = optionDAO.get(unknown_key);
        assertNotNull(value);
        assertEquals("unknown_value", value);

        optionDAO.delete(unknown_key);
        value = optionDAO.get(unknown_key);
        assertNull(value);
    }

    @Test
    void setTwiceTheSameKey() {
        String key1 = "key1";
        boolean isInserted = optionDAO.set(key1, "value1");
        assertTrue(isInserted);
        String value = optionDAO.get(key1);
        assertNotNull(value);
        assertEquals("value1", value);

        isInserted = optionDAO.set(key1, "value2");
        assertTrue(isInserted);
        value = optionDAO.get(key1);
        assertEquals("value2", value);

        optionDAO.delete(key1);
        value = optionDAO.get(key1);
        assertNull(value);
    }

    @Test
    void setWithDataSource() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        String unknown_key = "unknown_key_3";
        boolean isInserted = optionDAO.set(unknown_key, "unknown_value", dataSource);
        assertTrue(isInserted);
        String value = optionDAO.get(unknown_key, dataSource);
        assertNotNull(value);
        assertEquals("unknown_value", value);

        optionDAO.delete(unknown_key, dataSource);
        value = optionDAO.get(unknown_key, dataSource);
        assertNull(value);
    }

}