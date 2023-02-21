/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TrimDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TrimEntry;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.AnnotationLiteral;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TrimServiceTest {
    DatabaseManager databaseManager = mock(DatabaseManager.class);
    TrimDao trimDao = mock(TrimDao.class);
    TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
    Connection connection = mock(Connection.class);

    TrimService trimService;
    Event event = mock(Event.class);
    Event trimConfigEvent = mock(Event.class);
    DerivedTablesRegistry registry = mock(DerivedTablesRegistry.class);
    DerivedTableInterface derivedTable = mock(DerivedTableInterface.class);
    TimeService timeService = mock(TimeService.class);


    @BeforeEach
    void setUp() throws SQLException {
        trimService = new TrimService(databaseManager, registry, timeService, trimConfigEvent, trimDao, 1000);
        lenient().doReturn(dataSource).when(databaseManager).getDataSource();
        doReturn(connection).when(dataSource).getConnection();
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
        assertEquals(3000, lastTrimHeight);
    }

    @Test
    void testDoTrimDerivedTablesAndTriggerAsyncEvent() {
        Event firedEvent = mock(Event.class);
        doReturn(new TrimEntry(1L, 4000, false)).when(trimDao).save(new TrimEntry(null, 4000, false));
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<TrimEvent>() {
        });
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        doReturn(8100).when(timeService).getEpochTime();

        trimService.doTrimDerivedTablesOnBlockchainHeight(5000);

        verify(derivedTable).trim(4000);
    }

    @Test
    void testDoTrimDerivedTablesAtHeightLessThanMaxRollback() {
        trimService.doTrimDerivedTablesOnBlockchainHeight(999);

        verifyNoInteractions(trimDao);
    }

    @Test
    void testDoTrimDerivedTablesAtAlreadyScannedHeight() {
        Event firedEvent = mock(Event.class);
        doReturn(new TrimEntry(1L, 250000, true)).when(trimDao).get();

        trimService.doTrimDerivedTablesOnBlockchainHeight(250000);

        verify(trimDao, times(0)).clear();
        verifyNoInteractions(firedEvent);
    }

    @Test
    void testTrimDerivedTablesInOuterTransaction() {
        doReturn(true).when(dataSource).isInTransaction();
        doReturn(new TrimEntry(1L, 5000, false)).when(trimDao).save(new TrimEntry(null, 4000, false));
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(event).select(new AnnotationLiteral<TrimEvent>() {
        });
        doReturn(List.of(derivedTable)).when(registry).getDerivedTables();
        doReturn(3500).when(timeService).getEpochTime();

        trimService.trimDerivedTables(5000);

        verify(derivedTable).trim(4000);
        verify(dataSource, times(3)).commit(false);
    }


    @Test
    void testTrimDerivedTablesWithException() {
        doThrow(new RuntimeException()).when(trimDao).save(new TrimEntry(null, 6000, false));

        assertThrows(RuntimeException.class, () -> trimService.trimDerivedTables(6000));

        verify(dataSource).begin();
        verify(dataSource).rollback(true);
        verifyNoInteractions(derivedTable, event);
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

        trimService.doTrimDerivedTablesOnHeight(2000);

        verify(dataSource, times(2)).commit(false);
        verify(derivedTable, times(2)).trim(2000);
    }

    @Test
    void testWaitTrimming() {
        AtomicBoolean trimIsDone = new AtomicBoolean(false);
        AtomicBoolean trimBegan = new AtomicBoolean(false);
        doAnswer(invocationOnMock -> {
            trimBegan.set(true);
            while (!trimIsDone.get()) {
                ThreadUtils.sleep(10L);
            }
            return new TrimEntry(1L, 3000, true);
        }).when(trimDao).get();

        CompletableFuture<Void> trimmingTask = CompletableFuture.runAsync(() -> trimService.doAccountableTrimDerivedTables(2000)).handle((r, e)-> {
            if (e != null) {
                fail(e);
            }
            return r;
        });
        while (!trimBegan.get()) {
            ThreadUtils.sleep(10L);
        }
        CompletableFuture<Void> trimWaitingTask = CompletableFuture.runAsync(() -> {
            trimService.waitTrimming();
        });
        ThreadUtils.sleep(50);
        assertFalse(trimWaitingTask.isDone(), "TrimWaiting method cannot be finished during another trim operation");

        trimIsDone.set(true);
        trimmingTask.join();
        trimWaitingTask.join();
    }
}