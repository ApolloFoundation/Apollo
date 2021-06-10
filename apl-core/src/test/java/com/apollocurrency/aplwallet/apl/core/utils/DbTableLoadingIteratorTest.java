/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableData;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.entity.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import com.apollocurrency.aplwallet.apl.testutil.MockUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DbTableLoadingIteratorTest {
    @Mock
    EntityDbTableInterface<?> table;
    DerivedTestData td = new DerivedTestData();

    @Test
    void testLoadData() throws SQLException {
        doReturn(new MinMaxValue(BigDecimal.valueOf(td.ENTITY_1.getDbId()), BigDecimal.valueOf(td.ENTITY_4.getDbId()), "db_id", 4, td.ENTITY_4.getHeight())).when(table).getMinMaxValue(td.ENTITY_4.getHeight());
        Map<Integer, DerivedTableData<?>> loadingIterations = Map.of(1, new DerivedTableData<>(List.of(td.ENTITY_1, td.ENTITY_2), 2L),
            2, new DerivedTableData<>(List.of(td.ENTITY_3, td.ENTITY_4), 2L),
            3, new DerivedTableData<>(List.of(), 2));
        MockUtils.doAnswer(loadingIterations).when(table).getAllByDbId(anyLong(), anyInt(), anyLong());

        DbTableLoadingIterator<?> iterator = new DbTableLoadingIterator<>(table, 2, td.ENTITY_4.getHeight());

        assertEquals(List.of(td.ENTITY_1, td.ENTITY_2, td.ENTITY_3, td.ENTITY_4), endIterator(iterator));
        verify(table).getAllByDbId(td.ENTITY_1.getDbId(), 2, td.ENTITY_4.getDbId() + 1);
        verify(table).getAllByDbId(td.ENTITY_2.getDbId() + 1, 2, td.ENTITY_4.getDbId() + 1);
        verify(table).getAllByDbId(td.ENTITY_4.getDbId() + 1, 2, td.ENTITY_4.getDbId() + 1);
    }

    @Test
    void testLoadData_fetchLessThanLimit() throws SQLException {
        MockUtils.doAnswer(Map.of(1, new DerivedTableData<>(List.of(td.ENTITY_2), 2L))).when(table).getAllByDbId(anyLong(), anyInt(), anyLong());
        doReturn(new MinMaxValue(BigDecimal.valueOf(td.ENTITY_2.getDbId()), BigDecimal.valueOf(td.ENTITY_2.getDbId()), "db_id", 1, td.ENTITY_2.getHeight())).when(table).getMinMaxValue(td.ENTITY_2.getHeight());

        DbTableLoadingIterator<?> iterator = new DbTableLoadingIterator<>(table, 2, td.ENTITY_2.getHeight());

        assertEquals(List.of(td.ENTITY_2), endIterator(iterator));
        verify(table).getAllByDbId(td.ENTITY_2.getDbId(), 2, td.ENTITY_2.getDbId() + 1);
    }

    @Test
    void testLoadNothing() throws SQLException {
        MockUtils.doAnswer(Map.of(1, new DerivedTableData<>(List.of(), 2))).when(table).getAllByDbId(anyLong(), anyInt(), anyLong());
        doReturn(new MinMaxValue(BigDecimal.ZERO, BigDecimal.ZERO, "db_id", 0, 0)).when(table).getMinMaxValue(100);


        DbTableLoadingIterator<?> iterator = new DbTableLoadingIterator<>(table, 10, 100);

        assertEquals(List.of(), endIterator(iterator));
        verify(table).getAllByDbId(0, 10,  1);

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