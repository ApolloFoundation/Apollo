/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class InMemoryShufflingRepositoryTest extends ShufflingRepositoryTest {

    @Override
    public InMemoryShufflingRepository createRepository() {
        InMemoryShufflingRepository inMemoryShufflingRepository = new InMemoryShufflingRepository();
        inMemoryShufflingRepository.putAll(std.ALL_SHUFFLINGS);
        return inMemoryShufflingRepository;
    }

    @Test
    void testAnalyzeChangesForUnknownColumn() {
        assertThrows(IllegalArgumentException.class, () -> createRepository().analyzeChanges("unknown-column", "Prev value", std.SHUFFLING_3_3_APL_REGISTRATION));
    }

    @Test
    void testUpdateColumnsDuringRollback() {
        InMemoryShufflingRepository repository = createRepository();
        repository.rollback(std.SHUFFLING_4_1_APL_DONE.getHeight());
        Shuffling shuffling = repository.get(std.SHUFFLING_4_1_APL_DONE.getId());
        std.SHUFFLING_4_1_APL_DONE.setLatest(true);
        assertEquals(std.SHUFFLING_4_1_APL_DONE, shuffling);
    }

    @Test
    void testSetColumnForUnknownColumnName() {
        assertThrows(IllegalArgumentException.class, () -> createRepository().setColumn("unknown", "Value", std.SHUFFLING_3_3_APL_REGISTRATION));
    }

    @Test
    void testTruncate() {
        InMemoryShufflingRepository repository = createRepository();

        repository.truncate();

        assertEquals("Expected no shuffling entries after truncate", 0, repository.rowCount());
    }

    @Test
    void testDeleteAtHeight() throws CloneNotSupportedException {
        Shuffling shufflingToDelete = (Shuffling) std.SHUFFLING_8_1_CURRENCY_PROCESSING.clone();
        shufflingToDelete.setHeight(std.SHUFFLING_8_1_CURRENCY_PROCESSING.getHeight() + 1);
        shufflingToDelete.setDbId(std.NEW_SHUFFLING.getDbId() + 1);

        InMemoryShufflingRepository repository = createRepository();
        repository.deleteAtHeight(shufflingToDelete, shufflingToDelete.getHeight());

        Shuffling deletedShuffling = repository.get(std.SHUFFLING_8_1_CURRENCY_PROCESSING.getId());
        assertNull("Expected no deleted shuffling existence after deletion procedure", deletedShuffling);
    }

    @Test
    void testGetName() {
        String name = createRepository().getName();

        assertEquals("shuffling", name);
    }

    @Test
    void testGetRowCount() {
        int rowCount = createRepository().getRowCount();

        assertEquals(14, rowCount);
    }

    @Test
    void testIsMultiversion() {
        assertTrue("Shuffling in-memory table should support multiversion", createRepository().isMultiversion());
    }

    @Test
    void testThrowsExceptionOnDbMethods() {
        assertThrows(UnsupportedOperationException.class, () -> createRepository().isScanSafe());
        assertThrows(UnsupportedOperationException.class, () -> createRepository().prune(100));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getAllByDbId(0, 100, 200));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getRangeByDbId(mock(Connection.class), mock(PreparedStatement.class), mock(MinMaxValue.class), 2));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getMinMaxValue(2));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().supportDelete());
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getFullTextSearchColumns());
        assertThrows(UnsupportedOperationException.class, () -> createRepository().save(mock(Connection.class), std.SHUFFLING_3_3_APL_REGISTRATION));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().defaultSort());
        assertThrows(UnsupportedOperationException.class, () -> createRepository().get(new LongKey(1), true));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().get(new LongKey(1), 2));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getBy(DbClause.EMPTY_CLAUSE));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().get(mock(Connection.class), mock(PreparedStatement.class), false));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getManyBy(DbClause.EMPTY_CLAUSE, 0, -1));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getManyBy(DbClause.EMPTY_CLAUSE, 0, -1, "order by blah-blah..."));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getManyBy(DbClause.EMPTY_CLAUSE, 100, 0, -1));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getManyBy(DbClause.EMPTY_CLAUSE, 100, 0, -1, "order by blah-blah..."));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getManyBy(mock(Connection.class), mock(PreparedStatement.class), false));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getAll(0, -1));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getAll(0, -1, "order by blah-blah..."));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getCount(DbClause.EMPTY_CLAUSE));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getCount(DbClause.EMPTY_CLAUSE, 33));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getCount(mock(PreparedStatement.class)));
        assertThrows(UnsupportedOperationException.class, () -> createRepository().getAccountShufflings(1L, false, 0, 1));
    }
}