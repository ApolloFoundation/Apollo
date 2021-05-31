/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableData;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.entity.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import com.apollocurrency.aplwallet.apl.testutil.MockUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class DbTableLoadingIteratorTest {
    @Mock
    EntityDbTableInterface<?> table;
    DerivedTestData td = new DerivedTestData();

    @Test
    void testLoadData() throws SQLException {
        MockUtils.doAnswer(Map.of(1, new DerivedTableData<>(List.of(td.ENTITY_1, td.ENTITY_2), 2L), 2, new DerivedTableData<>(List.of(td.ENTITY_3, td.ENTITY_4), 2L), 3, new DerivedTableData<>(List.of(), 2))).when(table).getAllByDbId(anyLong(), anyInt(), anyLong());

        DbTableLoadingIterator<?> iterator = new DbTableLoadingIterator<>(table, 2);

        assertEquals(List.of(td.ENTITY_1, td.ENTITY_2, td.ENTITY_3, td.ENTITY_4), endIterator(iterator));
    }

    @Test
    void testLoadData_fetchLessThanLimit() throws SQLException {
        MockUtils.doAnswer(Map.of(1, new DerivedTableData<>(List.of(td.ENTITY_2), 2L))).when(table).getAllByDbId(anyLong(), anyInt(), anyLong());

        DbTableLoadingIterator<?> iterator = new DbTableLoadingIterator<>(table, 2);

        assertEquals(List.of(td.ENTITY_2), endIterator(iterator));
    }

    @Test
    void testLoadNothing() throws SQLException {
        MockUtils.doAnswer(Map.of(1, new DerivedTableData<>(List.of(), 2))).when(table).getAllByDbId(anyLong(), anyInt(), anyLong());

        DbTableLoadingIterator<?> iterator = new DbTableLoadingIterator<>(table);

        assertEquals(List.of(), endIterator(iterator));
    }

    private List<DerivedIdEntity> endIterator(DbTableLoadingIterator<?> iterator) {
        List<DerivedIdEntity> actualEntities = new ArrayList<>();
        while (iterator.hasNext()) {
            DerivedIdEntity next = (DerivedIdEntity) iterator.next();
            actualEntities.add(next);
        }
        return actualEntities;
    }

}