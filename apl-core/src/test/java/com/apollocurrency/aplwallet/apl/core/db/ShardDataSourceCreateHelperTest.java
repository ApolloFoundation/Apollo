package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@EnableWeld
class ShardDataSourceCreateHelperTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension();

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