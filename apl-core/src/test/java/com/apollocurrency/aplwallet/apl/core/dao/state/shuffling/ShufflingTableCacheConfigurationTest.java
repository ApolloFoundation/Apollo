/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableData;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ShufflingTableCacheConfigurationTest {
    @Mock
    TaskDispatchManager taskDispatchManager;
    @Mock
    TaskDispatcher taskDispatcher;
    @Mock
    ShufflingTable shufflingTable;

    ShufflingTableConfiguration configuration;
    ShufflingTestData td;


    void setUp(boolean enableCache) {
        configuration = new ShufflingTableConfiguration( taskDispatchManager, shufflingTable, enableCache);
        td = new ShufflingTestData();
    }


    @Test
    void initWithCache() throws SQLException {
        setUp(true);

        doReturn(taskDispatcher).when(taskDispatchManager).newScheduledDispatcher("ShufflingTableConfiguration-periodics");
        List<Shuffling> allShufflings = List.of(td.SHUFFLING_2_2_ASSET_REGISTRATION, td.SHUFFLING_1_2_APL_DONE_DELETED, td.SHUFFLING_7_1_CURRENCY_DONE);
        doReturn(new DerivedTableData<>(allShufflings, 0)).when(shufflingTable).getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE);

        configuration.init();

        ShufflingRepository table = configuration.getTable();
        assertTrue("Shuffling table should be cacheable, since the cache is enabled", table instanceof ShufflingCachedTable);
        List<Shuffling> actual = ((ShufflingCachedTable) table).getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(allShufflings, actual);
        verify(taskDispatcher).schedule(any(Task.class));

    }


    @Test
    void initWithoutCache() throws SQLException {
        setUp(false);

        configuration.init();

        ShufflingRepository table = configuration.getTable();
        assertTrue("Expected ShufflingTable type for non-cached shuffling table", table instanceof ShufflingTable);

        verifyNoInteractions(taskDispatchManager);
    }
}