/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.Change;
import com.apollocurrency.aplwallet.apl.core.db.model.DbIdLatestValue;
import com.apollocurrency.aplwallet.apl.core.db.model.EntityWithChanges;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedChangeableDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDeletableDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryVersionedDerivedEntityRepositoryTest {
    private DerivedTestData data;
    private List<VersionedChangeableDerivedEntity> insertedIntoRepoData;


    private InMemoryVersionedDerivedEntityRepository<VersionedChangeableDerivedEntity> repository = new InMemoryVersionedDerivedEntityRepository<>(new LongKeyFactory<VersionedChangeableDerivedEntity>("id") {
        @Override
        public DbKey newKey(VersionedChangeableDerivedEntity derivedIdEntity) {
            return new LongKey(derivedIdEntity.getId());
        }
    }, List.of("remaining")) {
        @Override
        public Value analyzeChanges(String columnName, Object prevValue, VersionedChangeableDerivedEntity entity) {
            if (!columnName.equals("remaining")) {
                throw new RuntimeException("Unknown column");
            }
            if (prevValue == null) {
                return new Value(entity.getRemaining());
            }
            int prevRemaining = ((int) prevValue);
            int currentRemaining = entity.getRemaining();
            if (prevRemaining != currentRemaining) {
                return new Value(currentRemaining);
            } else {
                return new Value();
            }
        }

        @Override
        public void setColumn(String columnName, Object value, VersionedChangeableDerivedEntity entity) {
            if (!columnName.equals("remaining")) {
                throw new RuntimeException("Unknown column");
            }
            entity.setRemaining((int) value);
        }
    };

    @BeforeEach
    void setUp() {
        data = new DerivedTestData();
        putTestData();
    }

    private void putTestData() {
        insertedIntoRepoData = data.ALL_VCDE.stream().map(e -> {
                try {
                    return e.clone();
                } catch (CloneNotSupportedException e1) {
                    throw new RuntimeException(e1);
                }
            }
        ).collect(Collectors.toList());
        repository.putAll(insertedIntoRepoData);
    }

    @Test
    void testGetDeleted() {
        VersionedDeletableDerivedIdEntity deleted = repository.get(new LongKey(4L));
        assertNull(deleted);
    }

    @Test
    void testGetEntityWhichNotExist() {
        VersionedDeletableDerivedIdEntity entity = repository.get(new LongKey(5L));
        assertNull(entity);
    }

    @Test
    void testGet() {
        VersionedChangeableDerivedEntity  entity = repository.get(new LongKey(2L));

        assertNotSame(data.VCDE_2_2, entity);
        assertEquals(data.VCDE_2_2, entity);
    }

    @Test
    void testGetCopy() {
        VersionedChangeableDerivedEntity  entity = repository.getCopy(new LongKey(1L));
        assertEquals(data.VCDE_1_3, entity);
        assertNotSame(data.VCDE_1_3, entity);
    }


    @Test
    void testInsertNew() {

        repository.insert(data.NEW_VCDE);

        VersionedChangeableDerivedEntity  savedEntity = repository.get(new LongKey(5L));
        assertNotSame(savedEntity, data.NEW_VCDE);
        assertTrue(data.NEW_VCDE.isLatest());
        assertEquals(9, repository.rowCount());
    }

    @Test
    void testInsertOnSameHeight() {
        data.VCDE_1_3.setRemaining(0);
        repository.insert(data.VCDE_1_3);
        VersionedChangeableDerivedEntity entity = repository.get(new LongKey(1L));
        assertEquals(data.VCDE_1_3, entity);
        EntityWithChanges<VersionedChangeableDerivedEntity> entityWithChanges = repository.getAllEntities().get(new LongKey(1L));
        Change remaining = entityWithChanges.getChanges().get("remaining").get(2);
        assertEquals(new Change(data.VCDE_1_3.getHeight(), 0), remaining);
        DbIdLatestValue dbIdLatestValue = entityWithChanges.getDbIdLatestValues().get(2);
        assertEquals(new DbIdLatestValue(data.VCDE_1_3.getHeight(), true, false, data.VCDE_1_3.getDbId()), dbIdLatestValue);
        assertEquals(8, repository.rowCount());
    }

    @Test
    void testInsertExisting() throws CloneNotSupportedException {
        VersionedChangeableDerivedEntity clone = data.VCDE_3_1.clone();
        clone.setHeight(data.VCDE_3_1.getHeight() + 10);
        clone.setDbId(data.VCDE_4_2.getDbId() + 1);

        repository.insert(clone);

        VersionedDeletableDerivedIdEntity savedEntity = repository.get(new LongKey(3L));
        assertNotSame(savedEntity, clone);
        assertTrue(clone.isLatest());
        assertFalse(data.VERSIONED_ENTITY_3_1.isLatest());
        assertEquals(9, repository.rowCount());
    }

    @Test
    void delete() throws CloneNotSupportedException {
        VersionedChangeableDerivedEntity clone = data.VCDE_1_3.clone();
        clone.setHeight(data.VCDE_1_3.getHeight() + 10);

        repository.delete(clone);

        VersionedDeletableDerivedIdEntity deleted = repository.get(new LongKey(1L));
        assertNull(deleted);
        assertFalse(clone.isLatest());
        assertFalse(data.VERSIONED_ENTITY_3_1.isLatest());
        assertEquals(9, repository.rowCount());
    }

    @Test
    void testDeleteWhichNotExist() {
        boolean delete = repository.delete(data.NEW_VCDE);

        assertFalse(delete);
        assertEquals(8, repository.rowCount());
    }

    @Test
    void testTrimAll() {
        repository.trim(data.VCDE_4_2.getHeight() + 1);
        EntityWithChanges<VersionedChangeableDerivedEntity> first = new EntityWithChanges<>(
                data.VCDE_1_3, Map.of("remaining",
                List.of(new Change(data.VCDE_1_3.getHeight(), data.VCDE_1_3.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_1_3.getHeight(), true, false, data.VCDE_1_3.getDbId())), data.VCDE_1_1.getHeight());
        EntityWithChanges<VersionedChangeableDerivedEntity> second = new EntityWithChanges<>(
                data.VCDE_2_2, Map.of("remaining",
                List.of(new Change(data.VCDE_2_2.getHeight(), data.VCDE_2_2.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_2_2.getHeight(), true, false, data.VCDE_2_2.getDbId())), data.VCDE_2_1.getHeight());

        EntityWithChanges<VersionedChangeableDerivedEntity> third = new EntityWithChanges<>(
                data.VCDE_3_1, Map.of("remaining",
                List.of(new Change(data.VCDE_3_1.getHeight(), data.VCDE_3_1.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_3_1.getHeight(), true, false, data.VCDE_3_1.getDbId())), data.VCDE_3_1.getHeight());


        Map<DbKey, EntityWithChanges<VersionedChangeableDerivedEntity>> expected = Map.of(
                new LongKey(1L), first,
                new LongKey(2L), second,
                new LongKey(3L), third
        );
        assertEquals(expected, repository.getAllEntities());
        assertEquals(3, repository.rowCount());
    }

    @Test
    void testTrimNothing() {
        repository.trim(data.VCDE_1_1.getHeight());

        Map<DbKey, EntityWithChanges<VersionedChangeableDerivedEntity>> expected = allExpected();
        assertEquals(expected, repository.getAllEntities());
        assertEquals(8, repository.rowCount());
    }


    @Test
    void testRollbackAll() {
        int removed = repository.rollback(0);

        assertEquals(8, removed);
        Map<DbKey, EntityWithChanges<VersionedChangeableDerivedEntity>> allEntities = repository.getAllEntities();
        assertEquals(0, allEntities.size());
        assertEquals(0, repository.rowCount());
    }

    @Test
    void testRollbackToFirstEntry() {
        int removed = repository.rollback(data.VCDE_1_1.getHeight());

        assertEquals(7, removed);
        data.VCDE_1_1.setLatest(true);
        EntityWithChanges<VersionedChangeableDerivedEntity> first = new EntityWithChanges<>(
                data.VCDE_1_1, Map.of("remaining",
                List.of(new Change(data.VCDE_1_1.getHeight(), data.VCDE_1_1.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_1_1.getHeight(), true, false, data.VCDE_1_1.getDbId())), data.VCDE_1_1.getHeight());
        assertEquals(Map.of(new LongKey(1L), first), repository.getAllEntities());
        assertEquals(1, repository.rowCount());
    }


    @Test
    void testRollbackNothing() {
        int removed = repository.rollback(data.VCDE_4_2.getHeight());

        assertEquals(0, removed);
        Map<DbKey, EntityWithChanges<VersionedChangeableDerivedEntity>> expected = allExpected();
        assertEquals(expected, repository.getAllEntities());
        assertEquals(8, repository.rowCount());
    }

    private Map<DbKey, EntityWithChanges<VersionedChangeableDerivedEntity>> allExpected() {
        EntityWithChanges<VersionedChangeableDerivedEntity> first = new EntityWithChanges<>(
                data.VCDE_1_3, Map.of("remaining",
                List.of(new Change(data.VCDE_1_1.getHeight(), data.VCDE_1_1.getRemaining()), new Change(data.VCDE_1_2.getHeight(), data.VCDE_1_2.getRemaining()), new Change(data.VCDE_1_3.getHeight(), data.VCDE_1_3.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_1_1.getHeight(), false, false, data.VCDE_1_1.getDbId()), new DbIdLatestValue(data.VCDE_1_2.getHeight(), false, false, data.VCDE_1_2.getDbId()), new DbIdLatestValue(data.VCDE_1_3.getHeight(), true, false, data.VCDE_1_3.getDbId())), data.VCDE_1_1.getHeight());
        EntityWithChanges<VersionedChangeableDerivedEntity> second = new EntityWithChanges<>(
                data.VCDE_2_2, Map.of("remaining",
                List.of(new Change(data.VCDE_2_1.getHeight(), data.VCDE_2_1.getRemaining()), new Change(data.VCDE_2_2.getHeight(), data.VCDE_2_2.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_2_1.getHeight(), false, false, data.VCDE_2_1.getDbId()), new DbIdLatestValue(data.VCDE_2_2.getHeight(), true, false, data.VCDE_2_2.getDbId())), data.VCDE_2_1.getHeight());

        EntityWithChanges<VersionedChangeableDerivedEntity> third = new EntityWithChanges<>(
                data.VCDE_3_1, Map.of("remaining",
                List.of(new Change(data.VCDE_3_1.getHeight(), data.VCDE_3_1.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_3_1.getHeight(), true, false, data.VCDE_3_1.getDbId())), data.VCDE_3_1.getHeight());
        EntityWithChanges<VersionedChangeableDerivedEntity> fourth = new EntityWithChanges<>(
                data.VCDE_4_2, Map.of("remaining",
                List.of(new Change(data.VCDE_4_1.getHeight(), data.VCDE_4_1.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_4_1.getHeight(), false, true, data.VCDE_4_1.getDbId()), new DbIdLatestValue(data.VCDE_4_2.getHeight(), false,true, data.VCDE_4_2.getDbId())), data.VCDE_4_1.getHeight());



        Map<DbKey, EntityWithChanges<VersionedChangeableDerivedEntity>> expected = Map.of(
                new LongKey(1L), first,
                new LongKey(2L), second,
                new LongKey(3L), third,
                new LongKey(4L), fourth
        );
        return expected;
    }

    @Test
    void testRollbackDeleted() {
        int removed = repository.rollback(227);

        assertEquals(3, removed);
        data.VCDE_1_2.setLatest(true);
        data.VCDE_2_1.setLatest(true);
        data.VCDE_4_1.setLatest(true);
        data.VCDE_4_1.setDeleted(false);
        EntityWithChanges<VersionedChangeableDerivedEntity> first = new EntityWithChanges<>(
                data.VCDE_1_2, Map.of("remaining",
                List.of(new Change(data.VCDE_1_1.getHeight(), data.VCDE_1_1.getRemaining()), new Change(data.VCDE_1_2.getHeight(), data.VCDE_1_2.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_1_1.getHeight(), false, false, data.VCDE_1_1.getDbId()), new DbIdLatestValue(data.VCDE_1_2.getHeight(), true, false, data.VCDE_1_2.getDbId())), data.VCDE_1_1.getHeight());
        EntityWithChanges<VersionedChangeableDerivedEntity> second = new EntityWithChanges<>(
                data.VCDE_2_1, Map.of("remaining",
                List.of(new Change(data.VCDE_2_1.getHeight(), data.VCDE_2_1.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_2_1.getHeight(), true, false, data.VCDE_2_1.getDbId())), data.VCDE_2_1.getHeight());

        EntityWithChanges<VersionedChangeableDerivedEntity> third = new EntityWithChanges<>(
                data.VCDE_3_1, Map.of("remaining",
                List.of(new Change(data.VCDE_3_1.getHeight(), data.VCDE_3_1.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_3_1.getHeight(), true, false, data.VCDE_3_1.getDbId())), data.VCDE_3_1.getHeight());
        EntityWithChanges<VersionedChangeableDerivedEntity> fourth = new EntityWithChanges<>(
                data.VCDE_4_1, Map.of("remaining",
                List.of(new Change(data.VCDE_4_1.getHeight(), data.VCDE_4_1.getRemaining()))),
                List.of(new DbIdLatestValue(data.VCDE_4_1.getHeight(), true, false, data.VCDE_4_1.getDbId())), data.VCDE_4_1.getHeight());



        Map<DbKey, EntityWithChanges<VersionedChangeableDerivedEntity>> expected = Map.of(
                new LongKey(1L), first,
                new LongKey(2L), second,
                new LongKey(3L), third,
                new LongKey(4L), fourth
        );
        assertEquals(expected, repository.getAllEntities());
    }

    @Test
    void testClear() {
        repository.clear();

        assertEquals(0, repository.getAllEntities().size());
    }

    @Test
    void testGetAll() {
        List<VersionedChangeableDerivedEntity> all = repository.getAll(Comparator.comparing(VersionedDeletableDerivedIdEntity::getId), 0, Integer.MAX_VALUE);

        assertEquals(List.of(data.VCDE_1_3, data.VCDE_2_2, data.VCDE_3_1), all);
    }

    @Test
    void testGetAllWithPagination() {
        List<VersionedChangeableDerivedEntity> all = repository.getAll(Comparator.comparing(VersionedDeletableDerivedIdEntity::getId), 0, 1);

        assertEquals(List.of(data.VCDE_1_3, data.VCDE_2_2), all);
    }

    @Test
    void testGetCopyWhichNotExists() {
        VersionedChangeableDerivedEntity nonexistentEntity = repository.getCopy(new LongKey(Long.MAX_VALUE));

        assertNull(nonexistentEntity);
    }

    @Test
    void testGetRowCount() {
        int count = repository.rowCount();

        assertEquals(8, count);
    }

    @Test
    void testGetAllRowsStream_withPagination() {
        List<VersionedChangeableDerivedEntity> entities = repository.getAllRowsStream(2, 4).collect(Collectors.toList());

        assertEquals(List.of(data.VCDE_2_1, data.VCDE_3_1, data.VCDE_4_1), entities);
    }


    @Test
    void testGetAllRowsStream() {
        List<VersionedChangeableDerivedEntity> entities = repository.getAllRowsStream(0, -1).collect(Collectors.toList());

        assertEquals(data.ALL_VCDE, entities);
    }
}