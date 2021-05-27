/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDeletableIdDerivedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FullyCachedTableTest {

    @Mock
    EntityDbTableInterface<VersionedDeletableIdDerivedEntity> dbTable;
    @Mock
    InMemoryVersionedDerivedEntityRepository<VersionedDeletableIdDerivedEntity> inMemoryRepo;

    FullyCachedTable<VersionedDeletableIdDerivedEntity> cachedTable;
    VersionedDeletableIdDerivedEntity entity = new VersionedDeletableIdDerivedEntity(1, 100L, 20, true);
    VersionedDeletableIdDerivedEntity entity2 = new VersionedDeletableIdDerivedEntity(2, 102L, 22, true);

    @BeforeEach
    void setUp() {
        cachedTable = new FullyCachedTable<>(inMemoryRepo, dbTable);
    }

    @Test
    void insert() {
        cachedTable.insert(entity);

        verify(dbTable).insert(entity);
        verify(inMemoryRepo).insert(entity);
    }

    @Test
    void get() {
        doReturn(entity).when(inMemoryRepo).get(new LongKey(entity.getId()));

        VersionedDeletableIdDerivedEntity actual = cachedTable.get(new LongKey(entity.getId()));

        assertEquals(entity, actual);

    }

    @Test
    void rollback() {
        doReturn(100).when(dbTable).rollback(2000);
        doReturn(22).when(inMemoryRepo).rowCount();
        doReturn(22).when(dbTable).getRowCount();

        int count = cachedTable.rollback(2000);

        assertEquals(100, count);
    }

    @Test
    void rollback_desync() throws SQLException {
        doReturn(100).when(dbTable).rollback(2000);
        doReturn(1).when(inMemoryRepo).rowCount();
        doReturn(2).when(dbTable).getRowCount();
        mockDumpOutput();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cachedTable.rollback(2000));

        verifyError(exception);
    }

    @Test
    void trim_desync() throws SQLException {
        doReturn(1).when(inMemoryRepo).rowCount();
        doReturn(2).when(dbTable).getRowCount();
        mockDumpOutput();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cachedTable.trim(2000));

        verifyError(exception);
    }

    @Test
    void deleteAtHeight() {
        doReturn(true).when(dbTable).deleteAtHeight(entity, entity.getHeight());
        doReturn(true).when(inMemoryRepo).delete(entity);

        boolean deleted = cachedTable.deleteAtHeight(entity, entity.getHeight());

        assertTrue(deleted, "Expected deleted=true for the inmem talbe and db table successful delete");
    }

    @Test
    void deleteAtHeight_desync() throws SQLException {
        doReturn(true).when(dbTable).deleteAtHeight(entity, entity.getHeight());
        doReturn(false).when(inMemoryRepo).delete(entity);
        doReturn("mock_table").when(dbTable).getName();
        mockDumpOutput();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cachedTable.deleteAtHeight(entity, entity.getHeight()));

        assertEquals("Desync of in-memory cache and the db, for the table mock_table after deletion of entity "  + entity.toString() + " at height " + entity.getHeight(), exception.getMessage());
    }

    @Test
    void truncate() {
        cachedTable.truncate();

        verify(dbTable).truncate();
        verify(inMemoryRepo).clear();
    }

    private void mockDumpOutput() throws SQLException {
        doReturn(new DerivedTableData<>(List.of(entity, entity2), 0)).when(dbTable).getAllByDbId(0, 100, Long.MAX_VALUE);
        doAnswer((Answer<Object>) invocation -> List.of(entity).stream()).when(inMemoryRepo).getAllRowsStream(0, -1);
    }

    private void verifyError(IllegalStateException exception) {
        String errorMessage = exception.toString();
        boolean containsDump = errorMessage.contains("db rows 2, mem rows 1");
        if (!containsDump) {
            fail(exception);
        }
    }
}