/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.prunable;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.PrunableMessageTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@EnableWeld
class PrunableMessageTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), null, null, "db/prunable-message-data.sql");
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
        PrunableMessageTable.class,
        DerivedDbTablesRegistryImpl.class,
        FullTextConfigImpl.class,
        BlockchainConfig.class,
        PropertiesHolder.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .build();
    @Inject
    PrunableMessageTable table;
    PrunableMessageTestData data = new PrunableMessageTestData();

    @Test
    void testGetByTransactionId() {
        long id = data.MESSAGE_1.getId();
        PrunableMessage prunableMessage = table.get(id);
        assertEquals(data.MESSAGE_1, prunableMessage);
    }

    @Test
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> table.insert(data.NEW_MESSAGE));

        PrunableMessage saved = table.get(data.NEW_MESSAGE.getId());
        assertNotNull(saved);
        assertEquals(data.NEW_MESSAGE, saved);
    }

    @Test
    void testInsertWithDbKey() {
        data.NEW_MESSAGE.setDbKey(new LongKey(data.NEW_MESSAGE.getId()));
        DbUtils.inTransaction(extension, (con) -> table.insert(data.NEW_MESSAGE));

        PrunableMessage saved = table.get(data.NEW_MESSAGE.getId());
        assertNotNull(saved);
        assertEquals(data.NEW_MESSAGE, saved);
    }

    @Test
    void testInsertWithoutMessages() {
        data.NEW_MESSAGE.setMessage(null);
        assertThrows(IllegalStateException.class, () -> DbUtils.inTransaction(extension, (con) -> table.insert(data.NEW_MESSAGE)));
    }

    @Test
    void testInsertWithEncryptedDataAndPlainMessage() {
        data.NEW_MESSAGE.setEncryptedData(data.DATA_1_ABTC);

        DbUtils.inTransaction(extension, (con) -> table.insert(data.NEW_MESSAGE));

        PrunableMessage saved = table.get(data.NEW_MESSAGE.getId());
        assertNotNull(saved);
        assertEquals(data.NEW_MESSAGE, saved);
    }

    @Test
    void testInsertWithEncryptedData() {
        data.NEW_MESSAGE.setEncryptedData(data.DATA_1_ABTC);
        data.NEW_MESSAGE.setMessage(null);
        DbUtils.inTransaction(extension, (con) -> table.insert(data.NEW_MESSAGE));

        PrunableMessage saved = table.get(data.NEW_MESSAGE.getId());
        assertNotNull(saved);
        assertEquals(data.NEW_MESSAGE, saved);
    }

    @Test
    void testGetAccountMessages() {
        List<PrunableMessage> prunableMessages = table.getPrunableMessages(data.ALICE_ID, 0, Integer.MAX_VALUE);
        List<PrunableMessage> expected = List.of(data.MESSAGE_11, data.MESSAGE_8, data.MESSAGE_6, data.MESSAGE_5, data.MESSAGE_4, data.MESSAGE_3, data.MESSAGE_2, data.MESSAGE_1);
        assertEquals(expected, prunableMessages);
    }

    @Test
    void testGetAccountMessagesWithPagination() {
        List<PrunableMessage> prunableMessages = table.getPrunableMessages(data.ALICE_ID, 2, 4);
        List<PrunableMessage> expected = List.of(data.MESSAGE_6, data.MESSAGE_5, data.MESSAGE_4);
        assertEquals(expected, prunableMessages);
    }

    @Test
    void testGetAccountMutualMessages() {
        List<PrunableMessage> prunableMessages = table.getPrunableMessages(data.BOB_ID, data.ALICE_ID, 0, Integer.MAX_VALUE);
        List<PrunableMessage> expected = List.of(data.MESSAGE_4, data.MESSAGE_3, data.MESSAGE_2, data.MESSAGE_1);
        assertEquals(expected, prunableMessages);
    }

    @Test
    void testGetAccountMutualMessagesWithPagination() {
        List<PrunableMessage> prunableMessages = table.getPrunableMessages(data.BOB_ID, data.ALICE_ID, 1, 2);
        List<PrunableMessage> expected = List.of(data.MESSAGE_3, data.MESSAGE_2);
        assertEquals(expected, prunableMessages);
    }

    @Test
    void testGetAllWithDefaultSort() {
        List<PrunableMessage> prunableMessages = CollectionUtil.toList(table.getAll(0, 2));
        assertEquals(List.of(data.MESSAGE_11, data.MESSAGE_10, data.MESSAGE_9), prunableMessages);
    }

    @Test
    void testIsPruned() {
        boolean pruned = table.isPruned(data.MESSAGE_7.getId(), false, true);
        assertFalse(pruned);
        pruned = table.isPruned(data.MESSAGE_6.getId(), true, false);
        assertFalse(pruned);
    }

    @Test
    void testIsPrunedForNonExistentMessage() {
        boolean pruned = table.isPruned(Long.MAX_VALUE, true, true);
        assertTrue(pruned);
    }

    @Test
    void testIsPrunedForMessageWithoutPtunableData() {
        boolean pruned = table.isPruned(data.MESSAGE_5.getId(), false, false);
        assertFalse(pruned);
    }

    @Test
    void testIsPrunedForMessageWithPrunableEncryptedDataAndWithoutPublicData() {
        boolean pruned = table.isPruned(data.MESSAGE_11.getId(), false, true);
        assertTrue(pruned);
    }

    @Test
    void testIsPrunedForEncryptedMessageWithoutPublic() {
        boolean pruned = table.isPruned(data.MESSAGE_8.getId(), true, false);
        assertTrue(pruned);
    }


}
