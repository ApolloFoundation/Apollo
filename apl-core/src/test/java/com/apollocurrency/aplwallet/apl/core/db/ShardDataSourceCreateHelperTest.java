package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@EnableWeld
class ShardDataSourceCreateHelperTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, ShardRecoveryDaoJdbcImpl.class, EpochTime.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();

    private ShardDataSourceCreateHelper createHelper;

    @Test
    void getShardId() {
        createHelper = new ShardDataSourceCreateHelper(extension.getDatabaseManger(), null);
        createHelper.createUninitializedDataSource();
        long shardId = createHelper.getShardId();
        assertEquals(3, shardId);
    }

    @Test
    void getShardName() {
        createHelper = new ShardDataSourceCreateHelper(extension.getDatabaseManger(), 1L);
        createHelper.createUninitializedDataSource();
        String shardName = createHelper.getShardName();
        assertEquals("apl-blockchain-shard-0000001", shardName);
    }

    @Test
    void getShardDb() {
        createHelper = new ShardDataSourceCreateHelper(extension.getDatabaseManger(), 2L);
        createHelper.createUninitializedDataSource();
        TransactionalDataSource transactionalDataSource = createHelper.getShardDb();
        assertNotNull(transactionalDataSource);
    }
}