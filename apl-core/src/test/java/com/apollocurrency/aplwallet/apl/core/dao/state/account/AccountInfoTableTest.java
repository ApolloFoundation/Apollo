/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.AnnotationLiteral;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@Tag("slow")
class AccountInfoTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, Map.of("account_info", List.of("name", "description")));
    AccountInfoTable table;
    AccountTestData testData = new AccountTestData();
    Event<FullTextOperationData> fullTextOperationDataEvent = mock(Event.class);

    @BeforeEach
    void setUp() {
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(fullTextOperationDataEvent);
        table = new AccountInfoTable(dbExtension.getDatabaseManager(), fullTextOperationDataEvent);
    }

    @Tag("skip-fts-init")
    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        dbExtension.cleanAndPopulateDb();
        AccountInfo previous = table.get(table.getDbKeyFactory().newKey(testData.newInfo));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newInfo));
        AccountInfo actual = table.get(table.getDbKeyFactory().newKey(testData.newInfo));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(testData.newInfo.getAccountId(), actual.getAccountId());
        assertEquals(testData.newInfo.getName(), actual.getName());
    }

    @Tag("skip-fts-init")
    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        dbExtension.cleanAndPopulateDb();
        AccountInfo previous = table.get(table.getDbKeyFactory().newKey(testData.ACC_INFO_0));
        assertNotNull(previous);
        previous.setName("Ping-Pong " + previous.getName());

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        AccountInfo actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertTrue(actual.getName().startsWith("Ping-Pong "));
        assertEquals(previous.getDescription(), actual.getDescription());
    }

    @Tag("skip-fts-init")
    @Test
    void testRollback() {
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(testData.ACC_INFO_0.getHeight()));
        verify(fullTextOperationDataEvent, times(4)).select(new AnnotationLiteral<TrimEvent>() {});
    }

}