/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ShardDataSourceCreateHelper;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j
@Testcontainers
@Tag("slow")
@EnableWeld
class ShardDataSourceCreateHelperTest {

    @Container
    public static final GenericContainer mariaDBContainer = new MariaDBContainer("mariadb:10.4")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withExposedPorts(3306)
        .withLogConsumer(new Slf4jLogConsumer(log));

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, ShardRecoveryDaoJdbcImpl.class, TimeServiceImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .build();

    private ShardDataSourceCreateHelper createHelper;

    @Test
    void getShardId() {
        createHelper = new ShardDataSourceCreateHelper(extension.getDatabaseManager(), null);
        createHelper.createUninitializedDataSource();
        long shardId = createHelper.getShardId();
        assertEquals(3, shardId);
    }

    @Test
    void getShardName() {
        createHelper = new ShardDataSourceCreateHelper(extension.getDatabaseManager(), 1L);
        createHelper.createUninitializedDataSource();
        String shardName = createHelper.getShardName();
        assertEquals("apl-blockchain-shard-1-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5", shardName);
    }

    @Test
    void getShardDb() {
        createHelper = new ShardDataSourceCreateHelper(extension.getDatabaseManager(), 2L);
        createHelper.createUninitializedDataSource();
        TransactionalDataSource transactionalDataSource = createHelper.getShardDb();
        assertNotNull(transactionalDataSource);
    }
}