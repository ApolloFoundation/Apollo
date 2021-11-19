/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.data.CurrencyTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;

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
class CurrencyTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/currency-data.sql", "db/schema.sql");

    CurrencyTable table;
    CurrencyTestData td;
    Event<FullTextOperationData> fullTextOperationDataEvent = mock(Event.class);

    @BeforeEach
    void setUp() {
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(fullTextOperationDataEvent);
        td = new CurrencyTestData();
        table = new CurrencyTable(dbExtension.getDatabaseManager(), fullTextOperationDataEvent);
    }

    @Test
    void testLoad() {
        Currency currencySupply = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_0));
        assertNotNull(currencySupply);
        assertEquals(td.CURRENCY_0, currencySupply);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        dbExtension.cleanAndPopulateDb();

        Currency currencySupply = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_NEW));
        assertNull(currencySupply);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        Currency previous = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.CURRENCY_NEW));
        Currency actual = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.CURRENCY_NEW.getId(), actual.getId());
        assertEquals(td.CURRENCY_NEW.getAccountId(), actual.getAccountId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        Currency previous = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_1));
        assertNotNull(previous);
        previous.setInitialSupply(previous.getInitialSupply() + 100);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        Currency actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertEquals(100, actual.getInitialSupply() - td.CURRENCY_1.getInitialSupply());
        assertEquals(previous.getId(), actual.getId());
        assertEquals(previous.getIssuanceHeight(), actual.getIssuanceHeight());
    }

    @Test
    void testRollback() {
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(td.CURRENCY_1.getHeight()));
        verify(fullTextOperationDataEvent, times(3)).select(new AnnotationLiteral<TrimEvent>() {});
    }

}