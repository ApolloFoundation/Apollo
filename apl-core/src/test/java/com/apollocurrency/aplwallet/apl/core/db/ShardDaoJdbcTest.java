/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_0;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import javax.inject.Inject;

@EnableWeld
class ShardDaoJdbcTest {

    @RegisterExtension
    DbExtension extension = new DbExtension();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, ShardDaoJdbcImpl.class, EpochTime.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();
    @Inject
    private ShardDaoJdbc daoJdbc;

    @Test
    void getMinMaxShardId() throws SQLException {
        assertNotNull(daoJdbc);
        MinMaxDbId minMaxDbId= daoJdbc.getMinMaxId(extension.getDatabaseManger().getDataSource(), SHARD_2.getShardHeight());
        assertNotNull(minMaxDbId );
        assertEquals(SHARD_0.getShardId() - 1, minMaxDbId.getMinDbId());
        assertEquals(SHARD_2.getShardId() + 1, minMaxDbId.getMaxDbId());
        assertEquals(3, minMaxDbId.getCount());
    }

}