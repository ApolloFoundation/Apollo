/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ValuesDbTableTest<T extends DerivedEntity> extends DerivedDbTableTest<T> {

    public ValuesDbTableTest(ValuesDbTable<T> derivedDbTable, Class<T> clazz) {
        super(derivedDbTable, clazz);
    }

    public ValuesDbTable<T> getTable() {
        return (ValuesDbTable<T>) derivedDbTable;
    }

    ValuesDbTable<T> table = getTable();

    @Test
    public void testGetByDbKey() {
        Map<DbKey, List<T>> heightValuesMap = groupByDbKey();
        Map.Entry<DbKey, List<T>> entry = heightValuesMap.entrySet().iterator().next();
        List<T> values = entry.getValue();
        DbKey dbKey = entry.getKey();
        List<T> result = table.get(dbKey);
        assertEquals(values, result);
    }

    @Test
    public void testGetByUnknownDbKey() {
        List<T> actual = table.get(new DbKey() {
            @Override
            public int setPK(PreparedStatement pstmt) throws SQLException {
                return setPK(pstmt, 1);
            }

            @Override
            public int setPK(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index, -1);
                return index + 1;
            }
        });
        assertTrue(actual.isEmpty(), "No records should be found at dbkey -1");

    }
    public Map<DbKey, List<T>> groupByDbKey() {
        return getAllExpectedData().stream().collect(Collectors.groupingBy(table.getDbKeyFactory()::newKey, collectingAndThen(toList(), (l -> l.stream().sorted().collect(toList())))));
    }
}
