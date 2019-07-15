/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class InMemoryDerivedEntityRepositoryTest {
    private DerivedIdEntity die1_1 = new DerivedIdEntity(null, 100, 1);
    private DerivedIdEntity die1_2 = new DerivedIdEntity(null, 101, 1);
    private DerivedIdEntity die1_3 = new DerivedIdEntity(null, 102, 1);
    private DerivedIdEntity die2_1 = new DerivedIdEntity(null, 100, 2);
    private DerivedIdEntity die2_2 = new DerivedIdEntity(null, 103, 2);
    private DerivedIdEntity die3_1 = new DerivedIdEntity(null, 99, 3);

    private InMemoryDerivedEntityRepository<DerivedIdEntity> repository = new InMemoryDerivedEntityRepository<DerivedIdEntity>(new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(DerivedIdEntity derivedIdEntity) {
            return new LongKey(derivedIdEntity.getId());
        }
    });

    @BeforeEach
    void setUp() {
        putTestData();
    }

    private void putTestData() {
        repository.putAll(List.of(
                die1_1,
                die1_2,
                die1_3,
                die2_1,
                die2_2,
                die3_1));
    }

    @Test
    void testGetAllEntities() {
        assertNothingChanged();
    }

    @Test
    void testClear() {
        repository.clear();

        Map<DbKey, List<DerivedIdEntity>> allEntities = repository.getAllEntities();
        assertEquals(0, allEntities.size());
    }

    @Test
    void testRollback() {
        repository.rollback(99);

        Map<DbKey, List<DerivedIdEntity>> allEntities = repository.getAllEntities();
        assertEquals(Map.of(new LongKey(die3_1.getId()), List.of(die3_1)), allEntities);
    }

    @Test
    void testRollbackToZeroHeight() {
        repository.rollback(0);
        Map<DbKey, List<DerivedIdEntity>> allEntities = repository.getAllEntities();
        assertEquals(0, allEntities.size());
    }

    @Test
    void testRollbackNothing() {
        repository.rollback(103);
        assertNothingChanged();
    }

    @Test
    void testGet() {
        DerivedIdEntity derivedIdEntity = repository.get(new LongKey(1L));
        assertSame(die1_3, derivedIdEntity);
        assertEquals(die1_3, derivedIdEntity);
    }

    @Test
    void testGetCopy() {
        DerivedIdEntity derivedIdEntity = repository.getCopy(new LongKey(2L));
        assertNotSame(die2_2, derivedIdEntity);
        assertEquals(die2_2, derivedIdEntity);
    }

    @Test
    void testGetCopyForEntityWhichNotExists() {
        DerivedIdEntity copy = repository.getCopy(new LongKey(5L));
        assertNull(copy, "Entity with id = 5 should not exist");
    }


    @Test
    void testInsertEntityWhichAlreadyExists() {
        DerivedIdEntity die = new DerivedIdEntity(null, 105, 3L);
        repository.insert(die);

        Map<LongKey, List<DerivedIdEntity>> expected = Map.of(new LongKey(die1_1.getId()), List.of(die1_1, die1_2, die1_3), new LongKey(die2_1.getId()), List.of(die2_1, die2_2), new LongKey(die3_1.getId()), List.of(die3_1, die));
        Map<DbKey, List<DerivedIdEntity>> actual = repository.getAllEntities();
        assertEquals(expected, actual);
    }

    @Test
    void testInsertEntityWhichAlreadyExistsWithSameHeight() {
        DerivedIdEntity die = new DerivedIdEntity(null, 103, 2L);
        repository.insert(die);

        Map<LongKey, List<DerivedIdEntity>> expected = Map.of(new LongKey(die1_1.getId()), List.of(die1_1, die1_2, die1_3), new LongKey(die2_1.getId()), List.of(die2_1, die), new LongKey(die3_1.getId()), List.of(die3_1));
        Map<DbKey, List<DerivedIdEntity>> actual = repository.getAllEntities();
        assertEquals(expected, actual);
        DerivedIdEntity entity = repository.get(new LongKey(2L));
        assertSame(entity, die);
    }

    @Test
    void testInsertNewEntity() {
        DerivedIdEntity die = new DerivedIdEntity(null, 103, 4L);
        repository.insert(die);

        Map<LongKey, List<DerivedIdEntity>> expected = Map.of(new LongKey(die1_1.getId()), List.of(die1_1, die1_2, die1_3), new LongKey(die2_1.getId()), List.of(die2_1, die2_2), new LongKey(die3_1.getId()), List.of(die3_1), new LongKey(4L), List.of(die));
        Map<DbKey, List<DerivedIdEntity>> actual = repository.getAllEntities();
        assertEquals(expected, actual);
    }

    @Test
    void delete() {
        repository.delete(die1_3);
        assertNothingChanged();
    }

    @Test
    void trim() {
        repository.trim(Integer.MAX_VALUE);
        assertNothingChanged();
    }

    private void assertNothingChanged() {
        Map<DbKey, List<DerivedIdEntity>> allEntities = repository.getAllEntities();
        Map<LongKey, List<DerivedIdEntity>> expected = Map.of(new LongKey(die1_1.getId()), List.of(die1_1, die1_2, die1_3), new LongKey(die2_1.getId()), List.of(die2_1, die2_2), new LongKey(die3_1.getId()), List.of(die3_1));
        assertEquals(expected, allEntities);
    }
}