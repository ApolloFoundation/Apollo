/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.prunable;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Tag("slow")
@ExtendWith(MockitoExtension.class)
class TaggedDataTableTest extends DbContainerBaseTest {
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/tagged-data.sql", null);
    TaggedDataTable table;
    @Mock
    DataTagDao dataTagDao;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    PropertiesHolder propertiesHolder;

    @BeforeEach
    void setUp() {
        table = new TaggedDataTable(dataTagDao, blockchainConfig, extension.getDatabaseManager(), propertiesHolder, mock(Event.class));
    }

    @Test
    void prune() throws AplException.NotValidException {
        doReturn(true).when(blockchainConfig).isEnablePruning();
        doReturn(2).when(propertiesHolder).BATCH_COMMIT_SIZE();
        doReturn(1000).when(blockchainConfig).getMaxPrunableLifetime();
        DbUtils.inTransaction(extension, (con)-> table.prune(3001));

        List<TaggedData> taggedDatas = CollectionUtil.toList(table.getAll(0, -1));
        assertEquals(taggedDatas.size(), 2);
        assertEquals("tag4", taggedDatas.get(1).getName());
        assertEquals("tag5", taggedDatas.get(0).getName());
        verify(dataTagDao).delete(Map.of("tag1", 1, "tag2", 2, "tag3", 2, "tag4", 1, "newtag", 1));

    }
}