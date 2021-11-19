/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedChangeableDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedChangeableNullableDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedDeletableDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryVersionedDerivedEntityRepositoryTest {
    private DerivedTestData data;
    private List<VersionedChangeableNullableDerivedEntity> insertedIntoRepoData;


    private InMemoryVersionedDerivedEntityRepository<VersionedChangeableNullableDerivedEntity> repository = new InMemoryVersionedDerivedEntityRepository<>(new LongKeyFactory<VersionedChangeableNullableDerivedEntity>("id") {
        @Override
        public DbKey newKey(VersionedChangeableNullableDerivedEntity derivedIdEntity) {
            return new LongKey(derivedIdEntity.getId());
        }
    }, List.of("remaining", "name", "description")) {
        @Override
        public Value analyzeChanges(String columnName, Object prevValue, VersionedChangeableNullableDerivedEntity entity) {
            switch (columnName) {
                case "remaining":
                    return ChangeUtils.getChange(entity.getRemaining(), prevValue);
                case "description":
                    return ChangeUtils.getChange(entity.getDescription(), prevValue);
                case "name":
                    return ChangeUtils.getChange(entity.getName(), prevValue);
                default:
                    throw new IllegalStateException("Unknown column name to analyze: " + columnName + " for value " + prevValue);
            }
        }

        @Override
        public void setColumn(String columnName, Object value, VersionedChangeableNullableDerivedEntity entity) {
            switch (columnName) {
                case "remaining":
                    entity.setRemaining((int) value);
                    break;
                case "description":
                    entity.setDescription((String) value);
                    break;
                case "name":
                    entity.setName((String) value);
                    break;
                default:
                    throw new IllegalStateException("Unknown column name to set: " + columnName + " for value " + value);
            }
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
    void testGetAllRowsCountByHeight() {
        int allCount = repository.rowCount(data.VCDE_4_2.getHeight());

        assertEquals(8, allCount);
    }

    @Test
    void testGetRowsCountByHeightInMiddle() {
        int count = repository.rowCount(data.VCDE_4_1.getHeight());

        assertEquals(5, count);
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
        data.VCDE_3_1.setRemaining(0);

        repository.insert(data.VCDE_3_1);

        VersionedChangeableDerivedEntity entity = repository.get(new LongKey(3L));
        assertEquals(data.VCDE_3_1, entity);
        assertEquals(8, repository.rowCount());
    }

    @Test
    void testInsertExisting() throws CloneNotSupportedException {
        VersionedChangeableNullableDerivedEntity clone = data.VCDE_3_1.clone();
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
        VersionedChangeableNullableDerivedEntity clone = data.VCDE_1_3.clone();
        clone.setHeight(data.VCDE_1_3.getHeight() + 10);
        clone.setDbId(data.VCDE_4_2.getDbId() + 2);

        repository.delete(clone);

        VersionedDeletableDerivedIdEntity deleted = repository.get(new LongKey(1L));
        assertNull(deleted);
        assertTrue(clone.isLatest()); // latest flag was not changed during deletion
        assertEquals(9, repository.rowCount());
        assertEquals(List.of(data.VCDE_3_1, data.VCDE_2_2), repository.getAll(Comparator.comparingLong(DerivedEntity::getDbId), 0, -1));
        clone.setLatest(false);
        clone.setDeleted(true);
        data.VCDE_1_3.setLatest(false);
        data.VCDE_1_3.setDeleted(true);
        ArrayList<VersionedChangeableNullableDerivedEntity> expected = new ArrayList<>(data.ALL_VCDE);
        expected.add(clone);
        assertEquals(expected, repository.getAllRowsStream(0, -1).collect(Collectors.toList()));
    }

    @Test
    void testDeleteOnTheFirstInsertHeight() {
        assertThrows(IllegalStateException.class, () -> repository.delete(data.VCDE_3_1));
    }

    @Test
    void testDeleteAndInsertAtTheSameHeightWithColumnChanges() throws CloneNotSupportedException {
        VersionedChangeableNullableDerivedEntity VCDE_2_3 = data.VCDE_2_2.clone();
        VCDE_2_3.setName(null);
        VCDE_2_3.setDescription("2_3");
        VCDE_2_3.setHeight(data.NEW_VCDE.getHeight() + 1);
        VCDE_2_3.setDbId(data.NEW_VCDE.getDbId() + 1);

        boolean deleted = repository.delete(VCDE_2_3);

        assertTrue("2_3 entity should be successfully deleted: " + VCDE_2_3, deleted);
        List<VersionedChangeableNullableDerivedEntity> allExpected = new ArrayList<>(data.ALL_VCDE);
        VCDE_2_3.setLatest(false);
        VCDE_2_3.setDeleted(true);
        data.VCDE_2_2.setDeleted(true);
        data.VCDE_2_2.setLatest(false);
        allExpected.add(VCDE_2_3);
        assertEntitiesEquals(allExpected);

        VCDE_2_3.setDescription(null);
        VCDE_2_3.setName("2_3 renewed name");
        VCDE_2_3.setRemaining(1000);
        VCDE_2_3.setDeleted(false);
        VCDE_2_3.setLatest(true);
        data.VCDE_2_2.setLatest(false);
        data.VCDE_2_2.setDeleted(false);

        repository.insert(VCDE_2_3); // merged insert / revert deleted

        assertEntitiesEquals(allExpected);
    }

    @Test
    void testDeleteMerge() throws CloneNotSupportedException {
        VersionedChangeableNullableDerivedEntity clone = data.VCDE_1_3.clone();
        clone.setName("deleted_name_1_3"); // deleted name should be saved

        repository.delete(clone);

        VersionedDeletableDerivedIdEntity deleted = repository.get(new LongKey(1L));
        assertNull(deleted);
        assertTrue(clone.isLatest()); // latest flag was not changed
        assertEquals(8, repository.rowCount());
        assertEquals(List.of(data.VCDE_3_1, data.VCDE_2_2), repository.getAll(Comparator.comparingLong(DerivedEntity::getDbId), 0, -1));
        data.VCDE_1_3.setLatest(false);
        data.VCDE_1_3.setDeleted(true);
        data.VCDE_1_3.setName(clone.getName());
        data.VCDE_1_2.setLatest(false);
        data.VCDE_1_2.setDeleted(true);
        assertEquals(data.ALL_VCDE, repository.getAllRowsStream(0, -1).collect(Collectors.toList()));
    }

    @Test
    void testDeleteAlreadyDeleted() throws CloneNotSupportedException {
        VersionedChangeableNullableDerivedEntity clone = data.VCDE_4_2.clone();

        boolean deleted = repository.delete(clone);

        assertFalse(deleted, "Expected unsuccessful deletion for the alread deleted entities");
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

        assertEquals(3, repository.rowCount());
        List<VersionedChangeableNullableDerivedEntity> expected = List.of(data.VCDE_3_1, data.VCDE_2_2, data.VCDE_1_3);
        assertEquals(expected, repository.getAll(Comparator.comparingLong(DerivedEntity::getDbId), 0, -1));
        assertEquals(expected, repository.getAllRowsStream(0, -1).collect(Collectors.toList()));

    }

    @Test
    void testTrimNothing() {
        repository.trim(data.VCDE_1_1.getHeight());

        assertEquals(data.ALL_VCDE, repository.getAllRowsStream(0, -1).collect(Collectors.toList()));
        assertEquals(8, repository.rowCount());
    }

    @Test
    void testTrim_keepDeleted() {
        repository.trim(data.VCDE_4_1.getHeight() + 1);

        assertEquals(List.of(data.VCDE_1_2, data.VCDE_2_1, data.VCDE_3_1, data.VCDE_4_1, data.VCDE_2_2, data.VCDE_1_3, data.VCDE_4_2),
            repository.getAllRowsStream(0, -1).collect(Collectors.toList()));
        assertEquals(7, repository.rowCount());
    }


    @Test
    void testRollbackAll() {
        int removed = repository.rollback(0);

        assertEquals(8, removed);
        assertEquals(0, repository.getAllRowsStream(0, -1).count());
        assertEquals(0, repository.rowCount());
    }

    @Test
    void testRollbackToFirstEntry() {
        int removed = repository.rollback(data.VCDE_1_1.getHeight());

        assertEquals(7, removed);
        data.VCDE_1_1.setLatest(true);

        assertEquals(List.of(data.VCDE_1_1), repository.getAll(Comparator.comparingLong(DerivedEntity::getDbId), 0, -1));
        assertEquals(List.of(data.VCDE_1_1), repository.getAllRowsStream(0, -1).collect(Collectors.toList()));
        assertEquals(1, repository.rowCount());
    }


    @Test
    void testRollbackNothing() {
        int removed = repository.rollback(data.VCDE_4_2.getHeight());

        assertEquals(0, removed);
        assertEquals(data.ALL_VCDE, repository.getAllRowsStream(0, -1).collect(Collectors.toList()));
        assertEquals(List.of(data.VCDE_3_1, data.VCDE_2_2, data.VCDE_1_3), repository.getAll(Comparator.comparingLong(DerivedEntity::getDbId), 0, -1));
        assertEquals(8, repository.rowCount());
    }

    @Test
    void testRollbackDeleted() {
        int removed = repository.rollback(227);

        assertEquals(3, removed);
        data.VCDE_1_2.setLatest(true);
        data.VCDE_2_1.setLatest(true);
        data.VCDE_4_1.setLatest(true);
        data.VCDE_4_1.setDeleted(false);

        assertEquals(5, repository.rowCount());
        assertEquals(List.of(data.VCDE_1_1, data.VCDE_1_2, data.VCDE_2_1, data.VCDE_3_1, data.VCDE_4_1), repository.getAllRowsStream(0,-1).collect(Collectors.toList()));
    }

    @Test
    void testClear() {
        repository.clear();

        assertEquals(0, repository.rowCount());
        assertEquals(0, repository.getAll(Comparator.comparingLong(DerivedEntity::getDbId), 0, -1).size());
        assertEquals(0, repository.getAllRowsStream(0, -1).count());
    }

    @Test
    void testGetAll() {
        List<VersionedChangeableNullableDerivedEntity> all = repository.getAll(Comparator.comparing(VersionedDeletableDerivedIdEntity::getId), 0, Integer.MAX_VALUE);

        assertEquals(List.of(data.VCDE_1_3, data.VCDE_2_2, data.VCDE_3_1), all);
    }

    @Test
    void testGetAllWithPagination() {
        List<VersionedChangeableNullableDerivedEntity> all = repository.getAll(Comparator.comparing(VersionedDeletableDerivedIdEntity::getId), 0, 1);

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

    @Test
    void testRealEntityFlow() throws CloneNotSupportedException {
        VersionedChangeableNullableDerivedEntity VCDE_3_2 = data.VCDE_3_1.clone();
        VCDE_3_2.setDescription("3_2");
        VCDE_3_2.setHeight(data.VCDE_3_1.getHeight() + 10);
        VCDE_3_2.setDbId(data.VCDE_4_2.getDbId() + 1);

        repository.insert(VCDE_3_2); // insert new versioned entity for the existing entity

        List<VersionedChangeableDerivedEntity> allRows = repository.getAllRowsStream(0, -1).collect(Collectors.toList());
        data.VCDE_3_1.setLatest(false);
        List<VersionedChangeableNullableDerivedEntity> expected = new ArrayList<>(data.ALL_VCDE);
        expected.add(VCDE_3_2);
        assertEquals(expected, allRows);


        VersionedChangeableNullableDerivedEntity VCDE_3_3 = VCDE_3_2.clone();
        VCDE_3_3.setDescription(null);
        VCDE_3_3.setHeight(VCDE_3_3.getHeight() + 1);
        VCDE_3_3.setDbId(VCDE_3_3.getDbId() + 1);

        repository.insert(VCDE_3_3);  // insert new versioned entity for the existing entity with the nullified description

        allRows = repository.getAllRowsStream(0, -1).collect(Collectors.toList());
        VCDE_3_2.setLatest(false);
        expected.add(VCDE_3_3);
        assertEquals(expected, allRows);

        repository.delete(VCDE_3_3); // do merged deletion for the same entity

        allRows = repository.getAllRowsStream(0, -1).collect(Collectors.toList());
        VCDE_3_3.setLatest(false);
        VCDE_3_3.setDeleted(true);
        VCDE_3_2.setDeleted(true);
        assertEquals(expected, allRows);

        VCDE_3_3.setName(null);
        VCDE_3_3.setLatest(true);
        VCDE_3_3.setDeleted(false);
        VCDE_3_2.setDeleted(false);
        VCDE_3_2.setLatest(false);

        repository.insert(VCDE_3_3);  // do merged insert (revert back deleted)

        allRows = repository.getAllRowsStream(0, -1).collect(Collectors.toList());
        assertRowsCount(10);
        assertEquals(expected, allRows);

        VersionedChangeableNullableDerivedEntity VCDE_3_4 = VCDE_3_3.clone();

        VCDE_3_3.setLatest(false);
        VCDE_3_4.setLatest(true);
        VCDE_3_4.setHeight(VCDE_3_4.getHeight() + 1);
        VCDE_3_4.setDbId(VCDE_3_4.getDbId() + 1);
        VCDE_3_4.setDeleted(false);
        VCDE_3_4.setName("3_4 name");
        VCDE_3_4.setDescription("3_4 description");

        repository.insert(VCDE_3_4);   // do insert new versioned entity

        allRows = repository.getAllRowsStream(0, -1).collect(Collectors.toList());
        assertRowsCount(11);
        expected.add(VCDE_3_4);
        assertEquals(expected, allRows);

        int removed = repository.rollback(VCDE_3_3.getHeight());  // rollback last versioned entity

        assertEquals(1, removed);
        assertRowsCount(10);
        allRows = repository.getAllRowsStream(0, -1).collect(Collectors.toList());
        expected.remove(expected.size() - 1);
        VCDE_3_3.setLatest(true);
        assertEquals(expected, allRows);

    }

    private void assertEntitiesEquals(List<VersionedChangeableNullableDerivedEntity> entities) {
        assertEquals(entities.size(), repository.rowCount());
        List<VersionedChangeableNullableDerivedEntity> allRows = repository.getAllRowsStream(0, -1).collect(Collectors.toList());
        assertEquals(entities, allRows);
    }

    private void assertRowsCount(int count) {
        assertEquals(count, repository.rowCount());
        assertEquals(count, repository.rowCount(Integer.MAX_VALUE));
    }
}