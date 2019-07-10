/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private InMemoryDerivedEntityRepository<DerivedIdEntity> repository = new InMemoryDerivedEntityRepository<DerivedIdEntity>(new LongKeyFactory<DerivedIdEntity>("id") {
        @Override
        public DbKey newKey(DerivedIdEntity derivedIdEntity) {
            return new LongKey(derivedIdEntity.getId());
        }
    });

    @BeforeEach
    void setUp() {
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
    void getAllEntities() {
        putTestData();
        Map<DbKey, List<DerivedIdEntity>> result = repository.getAllEntities();
        Map<LongKey, List<DerivedIdEntity>> expected = Map.of(new LongKey(die1_1.getId()), List.of(die1_1, die1_2, die1_3), new LongKey(die2_1.getId()), List.of(die2_1, die2_2), new LongKey(die3_1.getId()), List.of(die3_1));
        assertEquals(expected, result);
    }

    @Test
    void putAll() {
    }

    @Test
    void clear() {
    }

    @Test
    void rollback() {
    }

    @Test
    void get() {
    }

    @Test
    void getCopy() {
    }

    @Test
    void insert() {
    }

    @Test
    void doInsert() {
    }

    @Test
    void delete() {
    }

    @Test
    void trim() {
    }
}