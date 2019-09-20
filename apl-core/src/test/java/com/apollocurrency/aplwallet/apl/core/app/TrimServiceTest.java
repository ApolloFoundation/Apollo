/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Sync;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.shard.observer.TrimData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;

class TrimServiceTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    DatabaseManager databaseManager = spy(extension.getDatabaseManager());
    TrimDao trimDao = mock(TrimDao.class);

    TrimService trimService;
    Event event = mock(Event.class);
    DerivedTablesRegistry registry = mock(DerivedTablesRegistry.class);
    DerivedTableInterface derivedTable = mock(DerivedTableInterface.class);
    TimeService timeService = mock(TimeService.class);
    GlobalSync globalSync = spy(new GlobalSyncImpl());


    @BeforeEach
    void setUp() {
        trimService = new TrimService(databaseManager, registry, globalSync, timeService, event, trimDao, 1000);
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
        trimService.init(2000);
        verify(trimDao).save(new TrimEntry(null, 2000, true));
    }

    @Test
    void testInitWithExistingNotFinishedTrimEntry() {
        doReturn(new TrimEntry(1L, 5000, false)).when(trimDao).get();
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        TransactionalDataSource dataSource = spy(databaseManager.getDataSource());
        doReturn(dataSource).when(databaseManager).getDataSource();
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<Sync>() {});
        doReturn(new TrimEntry(1L, 5000, false)).when(trimDao).save(new TrimEntry(null, 5000, false));
        doReturn(7300).when(timeService).getEpochTime();

        trimService.init(5999);

        verify(globalSync).readLock();
        verify(globalSync).readUnlock();
        verify(trimDao).clear();
        verify(trimDao).save(new TrimEntry(null, 5000, false));
        verify(trimDao).save(new TrimEntry(1L, 5000, true));
        verify(dataSource).begin();
        verify(dataSource).commit(true);
        verify(firedEvent).fire(new TrimData(4000, 5000, 7200));
        verify(timeService).getEpochTime();
        verify(derivedTable).trim(4000);
    }

    @Test
    void testInitWithExistingOldTrimEntry() {
        doReturn(new TrimEntry(1L, 7000, true)).when(trimDao).get();
        doReturn(List.of(derivedTable, derivedTable)).when(registry).getDerivedTables();
        TransactionalDataSource dataSource = spy(databaseManager.getDataSource());
        doReturn(dataSource).when(databaseManager).getDataSource();
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<Sync>() {});
        mockTrimEntries(8000, 10000, 1000);
        doReturn(8000).when(timeService).getEpochTime();

        trimService.init(10500);

        verify(globalSync, times(6)).readLock();
        verify(globalSync, times(6)).readUnlock();
        verify(trimDao, times(3)).clear();
        verify(dataSource, times(3)).begin();
        verify(dataSource, times(3)).commit(true);
        verify(firedEvent).fire(new TrimData(7000, 8000, 7200));
        verify(firedEvent).fire(new TrimData(8000, 9000, 7200));
        verify(firedEvent).fire(new TrimData(9000, 10000, 7200));
        verify(timeService, times(3)).getEpochTime();
        verify(derivedTable, times(6)).trim(anyInt());
    }

    @Test
    void testInitWithExistingOldNotFinishedTrimEntry() {
        doReturn(new TrimEntry(1L, 10000, false)).when(trimDao).get();
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        TransactionalDataSource dataSource = spy(databaseManager.getDataSource());
        doReturn(dataSource).when(databaseManager).getDataSource();
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<Sync>() {});
        doReturn(7199).when(timeService).getEpochTime();
        mockTrimEntries(10000, 11000, 1000);

        trimService.init(11999);

        verify(globalSync, times(2)).readLock();
        verify(globalSync, times(2)).readUnlock();
        verify(trimDao, times(2)).clear();
        verify(dataSource, times(2)).begin();
        verify(dataSource, times(2)).commit(true);
        verify(firedEvent).fire(new TrimData(9000, 10000, 3600));
        verify(firedEvent).fire(new TrimData(10000, 11000, 3600));
        verify(derivedTable, times(2)).trim(anyInt());
        verify(derivedTable, times(2)).prune(3600);
    }

    private void mockTrimEntries(int initialHeight, int finishHeight, int step) {
        for (int i = initialHeight; i <= finishHeight; i += step) {
            doReturn(new TrimEntry(1L, i, false)).when(trimDao).save(new TrimEntry(null, i, false));
        }
    }

    @Test
    void testDoTrimDerivedTablesAndTriggerAsyncEvent() {
        Event firedEvent = mock(Event.class);
        doReturn(new TrimEntry(1L, 5000, false)).when(trimDao).save(new TrimEntry(null, 5000, false));
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<Async>() {});
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        doReturn(8100).when(timeService).getEpochTime();

        DbUtils.inTransaction(extension, con -> trimService.doTrimDerivedTablesOnBlockchainHeight(5000, true));

        verify(derivedTable).trim(4000);
        verify(firedEvent).fire(new TrimData(4000, 5000, 7200));
    }

    @Test
    void testDoTrimDerivedTablesAtHeightLessThanMaxRollback() {
        trimService.doTrimDerivedTablesOnBlockchainHeight(999, true);

        verifyZeroInteractions(trimDao);
    }


    @Test
    void testTrimDerivedTablesInOuterTransaction() {
        databaseManager.getDataSource().begin();
        doReturn(new TrimEntry(1L, 5000, false)).when(trimDao).save(new TrimEntry(null, 5000, false));
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<Async>() {});
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        doReturn(3500).when(timeService).getEpochTime();

        trimService.trimDerivedTables(5000, true);

        assertTrue(databaseManager.getDataSource().isInTransaction());
        verify(derivedTable).trim(4000);
        verify(firedEvent).fire(new TrimData(4000, 5000, 0));
    }


    @Test
    void testTrimDerivedTablesWithException() {
        doThrow(new RuntimeException()).when(trimDao).save(new TrimEntry(null, 6000, false));
        TransactionalDataSource dataSource = spy(databaseManager.getDataSource());
        doReturn(dataSource).when(databaseManager).getDataSource();

        assertThrows(RuntimeException.class, () -> trimService.trimDerivedTables(6000, false));

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
    void testDoTrimDerivedTablesOnHeight() {
        doReturn(List.of(derivedTable, derivedTable)).when(registry).getDerivedTables();
        TransactionalDataSource dataSource = spy(databaseManager.getDataSource());
        doReturn(dataSource).when(databaseManager).getDataSource();

        DbUtils.inTransaction(extension, con -> trimService.doTrimDerivedTablesOnHeight(2000, true));

//        verify(globalSync, times(1)).readLock();
//        verify(globalSync, times(1)).readUnlock();
        verify(dataSource, times(2)).commit(false);

        verify(derivedTable, times(2)).trim(2000);
    }
}