/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import static com.apollocurrency.aplwallet.apl.crypto.Convert.parseHexString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
class ShardDaoTest {

    private static PropertiesHolder propertiesHolder;
    private DbProperties dbProperties;
    @Inject
    private DaoConfig daoConfig;
    private static DatabaseManager databaseManager;
    private static Jdbi jdbi;
    @Inject
    private JdbiHandleFactory jdbiHandleFactory;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class, DaoConfig.class,
            JdbiHandleFactory.class, ShardDao.class,
            GlobalSync.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistry.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class)
//            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .build();

    @Inject
    private ShardDao dao;

    @BeforeAll
    static void setup() {
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                null,
                false,
                null,
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        propertiesHolder.init(propertiesLoader.load());
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        databaseManager = new DatabaseManager(dbConfig.getDbConfig(), propertiesHolder);
    }

    @AfterAll
    static void cleanup() {
//        databaseManager.shutdown();
    }

    @BeforeEach
    void setUp() {
        jdbi = databaseManager.getJdbi();
        jdbiHandleFactory.setJdbi(jdbi);
        daoConfig.setJdbiHandleFactory(jdbiHandleFactory);
    }

    @Test
    void insertDelete() {
        long maxId = dao.getMaxShardId();
        assertEquals(1, maxId);

        Shard shard = new Shard(1L, "aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f");
        dao.saveShard(shard);
        List<Shard> result = dao.getAllShard();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getShardId());
        assertNotNull(result.get(0).getShardHash());

        long count = dao.countShard();
        assertEquals(1, count);
        maxId = dao.getMaxShardId();
        assertEquals(2, maxId);

        long nextId = dao.getNextShardId();
        assertEquals(2, nextId);

        Shard shard2 = new Shard(2L, "0000002");
        dao.saveShard(shard2);

        maxId = dao.getMaxShardId();
        assertEquals(3, maxId);

        Shard found1 = dao.getShardById(1L);
        assertNotNull(found1);
        assertNotNull(found1.getShardId());
        assertArrayEquals(parseHexString("aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f"),
                found1.getShardHash() );

        found1.setShardHash("000000123".getBytes());
        dao.updateShard(found1);

        Shard found3 = dao.getShardById(1L);
        assertNotNull(found3);
        assertArrayEquals("000000123".getBytes(), found3.getShardHash());

        dao.hardDeleteShard(1L);
        count = dao.countShard();
        assertEquals(1, count);

        dao.hardDeleteAllShards();
    }
}