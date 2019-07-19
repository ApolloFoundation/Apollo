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

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

class InMemoryVersionedDerivedEntityRepositoryTest {
    DerivedTestData data;


    private InMemoryVersionedDerivedEntityRepository<VersionedDerivedIdEntity> repository = new InMemoryVersionedDerivedEntityRepository<>(new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(VersionedDerivedIdEntity derivedIdEntity) {
            return new LongKey(derivedIdEntity.getId());
        }
    });

    @BeforeEach
    void setUp() {
        data = new DerivedTestData();
        putTestData();
    }

    private void putTestData() {
        repository.putAll(data.ALL_VERSIONED);
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
        assertEquals(data.VERSIONED_ENTITY_3_2, entity);
        assertSame(data.VERSIONED_ENTITY_3_2, entity);
    }

    @Test
    void testGetCopy() {
        VersionedDerivedIdEntity entity = repository.getCopy(new LongKey(3L));
        assertEquals(data.VERSIONED_ENTITY_3_2, entity);
        assertNotSame(data.VERSIONED_ENTITY_3_2, entity);
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
        assertFalse(data.VERSIONED_ENTITY_3_1.isLatest());
    }

    @Test
    void delete() {
        VersionedDerivedIdEntity entity = new VersionedDerivedIdEntity(null, 105, 3L, true);
        repository.delete(entity);

        VersionedDerivedIdEntity deleted = repository.get(new LongKey(3L));
        assertNull(deleted);
        assertFalse(entity.isLatest());
        assertFalse(data.VERSIONED_ENTITY_3_1.isLatest());
    }

    @Test
    void testTrimAll() {
        repository.trim(104);
        Map<DbKey, List<VersionedDerivedIdEntity>> expected = Map.of(new LongKey(1L), List.of(data.VERSIONED_ENTITY_1_1), new LongKey(2L), List.of(data.VERSIONED_ENTITY_2_3), new LongKey(3L), List.of(data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_3_2));
        assertEquals(expected, repository.getAllEntities());
    }


    @Test
    void testRollback() {
        repository.rollback(102);
        Map<DbKey, List<VersionedDerivedIdEntity>> expected = Map.of(new LongKey(3L), List.of(data.VERSIONED_ENTITY_3_1), new LongKey(1L), List.of(data.VERSIONED_ENTITY_1_1), new LongKey(2L), List.of(data.VERSIONED_ENTITY_2_1, data.VERSIONED_ENTITY_2_2, data.VERSIONED_ENTITY_2_3), new LongKey(4L), List.of(data.VERSIONED_ENTITY_4_1, data.VERSIONED_ENTITY_4_2));
        assertEquals(expected, repository.getAllEntities());
        assertFalse(data.VERSIONED_ENTITY_4_2.isLatest()); // deleted record remains still deleted after rollback
        assertTrue(data.VERSIONED_ENTITY_3_1.isLatest());
    }

    @Test
    void testRollbackDeleted() {
        repository.rollback(101);
        Map<DbKey, List<VersionedDerivedIdEntity>> expected = Map.of(new LongKey(2L), List.of(data.VERSIONED_ENTITY_2_1, data.VERSIONED_ENTITY_2_2), new LongKey(3L), List.of(data.VERSIONED_ENTITY_3_1), new LongKey(1L), List.of(data.VERSIONED_ENTITY_1_1), new LongKey(4L), List.of(data.VERSIONED_ENTITY_4_1));
        assertEquals(expected, repository.getAllEntities());
        assertTrue(data.VERSIONED_ENTITY_4_1.isLatest());
        assertTrue(data.VERSIONED_ENTITY_3_1.isLatest());
        assertTrue(data.VERSIONED_ENTITY_2_2.isLatest());
    }

    @Test
    void testClear() {
        repository.clear();

        assertEquals(0, repository.getAllEntities().size());
    }

    @Test
    void testGetAll() {
        List<VersionedDerivedIdEntity> all = repository.getAll(Comparator.comparing(VersionedDerivedIdEntity::getId), 0, Integer.MAX_VALUE);

        assertEquals(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), all);
    }

    @Test
    void testGetAllWithPagination() {
        List<VersionedDerivedIdEntity> all = repository.getAll(Comparator.comparing(VersionedDerivedIdEntity::getId), 0, 1);

        assertEquals(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3), all);
    }

    @Test
    void testGetCopyWhichNotExists() {
        VersionedDerivedIdEntity nonexistentEntity = repository.getCopy(new LongKey(Long.MAX_VALUE));
        
        assertNull(nonexistentEntity);
    }

}