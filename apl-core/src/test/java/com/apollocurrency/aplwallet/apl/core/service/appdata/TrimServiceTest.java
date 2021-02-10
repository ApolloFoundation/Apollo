/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TrimDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TrimEntry;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.TrimData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

class TrimServiceTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    DatabaseManager databaseManager = spy(extension.getDatabaseManager());
    TrimDao trimDao = mock(TrimDao.class);

    TrimService trimService;
    Event event = mock(Event.class);
    Event trimConfigEvent = mock(Event.class);
    DerivedTablesRegistry registry = mock(DerivedTablesRegistry.class);
    DerivedTableInterface derivedTable = mock(DerivedTableInterface.class);
    TimeService timeService = mock(TimeService.class);


    @BeforeEach
    void setUp() {
        trimService = new TrimService(databaseManager, registry, timeService, trimConfigEvent, trimDao, 1000);
    }

    @Test
    void testGetLastTrimHeightWhenTrimEntryIsNull() {
        int lastTrimHeight = trimService.getLastTrimHeight();
        assertEquals(0, lastTrimHeight);
    }

    @Test
    void testGetLastTrimHeightWhenTrimEntryIsNotNull() {
        doReturn(new TrimEntry(1L, 3000, true)).when(trimDao).get();
        int lastTrimHeight = trimService.getLastTrimHeight();
        assertEquals(2000, lastTrimHeight);
    }

    @Test
    void testInitWithNullTrimEntry() {
//        trimService.init(2000, 0);
        verify(trimDao).save(new TrimEntry(null, 2000, true));
    }

    @Test
    void testDoTrimDerivedTablesAndTriggerAsyncEvent() {
        Event firedEvent = mock(Event.class);
        doReturn(new TrimEntry(1L, 5000, false)).when(trimDao).save(new TrimEntry(null, 5000, false));
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<TrimEvent>() {
        });
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        doReturn(8100).when(timeService).getEpochTime();

        DbUtils.inTransaction(extension, con -> trimService.doTrimDerivedTablesOnBlockchainHeight(5000));

        verify(derivedTable).trim(4000);
        verify(firedEvent, never()).fireAsync(new TrimData(4000, 5000, 7200));
    }

    @Test
    void testDoTrimDerivedTablesAtHeightLessThanMaxRollback() {
        trimService.doTrimDerivedTablesOnBlockchainHeight(999);

        verifyZeroInteractions(trimDao);
    }

    @Test
    void testDoTrimDerivedTablesAtAlreadyScannedHeight() {
        Event firedEvent = mock(Event.class);
        doReturn(new TrimEntry(1L, 250000, true)).when(trimDao).get();

        DbUtils.inTransaction(extension, con -> trimService.doTrimDerivedTablesOnBlockchainHeight(250000));

        verify(trimDao, times(0)).clear();
        verifyZeroInteractions(firedEvent);
    }

    @Test
    void testTrimDerivedTablesInOuterTransaction() {
        databaseManager.getDataSource().begin();
        doReturn(new TrimEntry(1L, 5000, false)).when(trimDao).save(new TrimEntry(null, 5000, false));
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<TrimEvent>() {
        });
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        doReturn(3500).when(timeService).getEpochTime();

        trimService.trimDerivedTables(5000);

        assertTrue(databaseManager.getDataSource().isInTransaction());
        verify(derivedTable).trim(4000);
        verify(firedEvent, never()).fireAsync(new TrimData(4000, 5000, 0));
    }


    @Test
    void testTrimDerivedTablesWithException() {
        doThrow(new RuntimeException()).when(trimDao).save(new TrimEntry(null, 6000, false));
        TransactionalDataSource dataSource = spy(databaseManager.getDataSource());
        doReturn(dataSource).when(databaseManager).getDataSource();

        assertThrows(RuntimeException.class, () -> trimService.trimDerivedTables(6000));

        assertFalse(dataSource.isInTransaction());
        verify(dataSource).begin();
        verify(dataSource).rollback(true);
        verifyZeroInteractions(derivedTable, event);
    }

    @Test
    void testResetTrim() {
        trimService.resetTrim();

        verify(trimDao).clear();
    }

    @Test
    void testResetTrimToHeight() {
        trimService.resetTrim(2000);
        verify(trimDao).clear();
        verify(trimDao).save(new TrimEntry(null, 2000, true));
    }

    @Test
    void testDoTrimDerivedTablesOnHeight() {
        doReturn(List.of(derivedTable, derivedTable)).when(registry).getDerivedTables();
        TransactionalDataSource dataSource = spy(databaseManager.getDataSource());
        doReturn(dataSource).when(databaseManager).getDataSource();

        DbUtils.inTransaction(extension, con -> trimService.doTrimDerivedTablesOnHeight(2000));

//        verify(globalSync, times(1)).readLock();
//        verify(globalSync, times(1)).readUnlock();
        verify(dataSource, times(2)).commit(false);

        verify(derivedTable, times(2)).trim(2000);
    }
}