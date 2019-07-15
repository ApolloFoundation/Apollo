/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class InMemoryVersionedDerivedEntityRepositoryTest {
    private VersionedDerivedIdEntity die1_1 = new VersionedDerivedIdEntity(null, 100, 1L, false);
    private VersionedDerivedIdEntity die1_2 = new VersionedDerivedIdEntity(null, 101, 1L, false);
    private VersionedDerivedIdEntity die1_3 = new VersionedDerivedIdEntity(null, 102, 1L, true);
    private VersionedDerivedIdEntity die2_1 = new VersionedDerivedIdEntity(null, 100, 2L, false);
    private VersionedDerivedIdEntity die2_2 = new VersionedDerivedIdEntity(null, 103, 2L, true);
    private VersionedDerivedIdEntity die3_1 = new VersionedDerivedIdEntity(null, 99 , 3L, true);
    private VersionedDerivedIdEntity die4_1 = new VersionedDerivedIdEntity(null, 101, 4L, false);
    private VersionedDerivedIdEntity die4_2 = new VersionedDerivedIdEntity(null, 102, 4L, false);


    private InMemoryVersionedDerivedEntityRepository<VersionedDerivedIdEntity> repository = new InMemoryVersionedDerivedEntityRepository<>(new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(VersionedDerivedIdEntity derivedIdEntity) {
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
                die3_1,
                die4_1,
                die4_2));
    }

    @Test
    void testGetDeleted() {
        VersionedDerivedIdEntity deleted = repository.get(new LongKey(4L));
        assertNull(deleted);
    }

    @Test
    void testGetEntityWhichNotExist() {
        VersionedDerivedIdEntity entity = repository.get(new LongKey(5L));
        assertNull(entity);
    }

    @Test
    void testGet() {
        VersionedDerivedIdEntity entity = repository.get(new LongKey(3L));
        assertEquals(die3_1, entity);
        assertSame(die3_1, entity);
    }

    @Test
    void testGetCopy() {
        VersionedDerivedIdEntity entity = repository.getCopy(new LongKey(3L));
        assertEquals(die3_1, entity);
        assertNotSame(die3_1, entity);
    }


    @Test
    void testInsertNew() {
        VersionedDerivedIdEntity entity = new VersionedDerivedIdEntity(null, 105, 5L, false);

        repository.insert(entity);

        VersionedDerivedIdEntity savedEntity = repository.get(new LongKey(5L));
        assertSame(savedEntity, entity);
        assertTrue(entity.isLatest());
    }

    @Test
    void testInsertExisting() {
        VersionedDerivedIdEntity entity = new VersionedDerivedIdEntity(null, 105, 3L, false);

        repository.insert(entity);

        VersionedDerivedIdEntity savedEntity = repository.get(new LongKey(3L));
        assertSame(savedEntity, entity);
        assertTrue(entity.isLatest());
        assertFalse(die3_1.isLatest());
    }

    @Test
    void delete() {
        VersionedDerivedIdEntity entity = new VersionedDerivedIdEntity(null, 105, 3L, true);
        repository.delete(entity);

        VersionedDerivedIdEntity deleted = repository.get(new LongKey(3L));
        assertNull(deleted);
        assertFalse(entity.isLatest());
        assertFalse(die3_1.isLatest());
    }

    @Test
    void testTrimAll() {
        repository.trim(104);
        Map<DbKey, List<VersionedDerivedIdEntity>> expected = Map.of(new LongKey(1L), List.of(die1_3), new LongKey(2L), List.of(die2_2), new LongKey(3L), List.of(die3_1));
        assertEquals(expected, repository.getAllEntities());
    }


    @Test
    void testRollback() {
        repository.rollback(102);
        Map<DbKey, List<VersionedDerivedIdEntity>> expected = Map.of(new LongKey(1L), List.of(die1_1, die1_2, die1_3), new LongKey(2L), List.of(die2_1), new LongKey(3L), List.of(die3_1), new LongKey(4L), List.of(die4_1, die4_2));
        assertEquals(expected, repository.getAllEntities());
        assertFalse(die4_2.isLatest());
        assertTrue(die2_1.isLatest());
    }

    @Test
    void testRollbackDeleted() {
        repository.rollback(101);
        Map<DbKey, List<VersionedDerivedIdEntity>> expected = Map.of(new LongKey(1L), List.of(die1_1, die1_2), new LongKey(2L), List.of(die2_1), new LongKey(3L), List.of(die3_1), new LongKey(4L), List.of(die4_1));
        assertEquals(expected, repository.getAllEntities());
        assertTrue(die4_1.isLatest());
        assertTrue(die2_1.isLatest());
        assertTrue(die1_2.isLatest());
    }

}