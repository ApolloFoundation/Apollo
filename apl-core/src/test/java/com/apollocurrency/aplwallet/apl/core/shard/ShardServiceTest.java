/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_1;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_2;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.data.ShardTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.List;

public class ShardServiceTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    ShardService shardService;

    @BeforeEach
    void setUp() {
        shardService = new ShardService();
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory();
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        ShardDao shardDao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, ShardDao.class);
        shardService.setShardDao(shardDao);
    }

    @Test
    void testGetAllShards() {
        List<Shard> allShards = shardService.getAllShards();
        assertEquals(ShardTestData.SHARDS, allShards);
    }

    @Test
    void testGetAllCompletedOrArchivedShards() {
        List<Shard> allShards = shardService.getAllCompletedOrArchivedShards();
        assertEquals(Arrays.asList(SHARD_2, SHARD_1), allShards);
    }

    @Test
    void testGetAllCompletedShards() {
        List<Shard> allShards = shardService.getAllCompletedShards();
        assertEquals(Arrays.asList(SHARD_1), allShards);
    }

}
