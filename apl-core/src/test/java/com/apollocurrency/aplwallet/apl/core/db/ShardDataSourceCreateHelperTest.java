/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ShardDataSourceCreateHelper;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@QuarkusTest
class ShardDataSourceCreateHelperTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

/*
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, ShardRecoveryDaoJdbcImpl.class, TimeServiceImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .build();
*/

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
        assertEquals("apl_blockchain_b5d7b6_shard_1", shardName);
    }

    @Test
    void getShardDb() {
        createHelper = new ShardDataSourceCreateHelper(extension.getDatabaseManager(), 2L);
        createHelper.createUninitializedDataSource();
        TransactionalDataSource transactionalDataSource = createHelper.getShardDb();
        assertNotNull(transactionalDataSource);
    }
}