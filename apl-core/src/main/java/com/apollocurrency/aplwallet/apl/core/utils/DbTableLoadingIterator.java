/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class DbTableLoadingIterator<T extends DerivedEntity> implements Iterator<T> {
    private final DerivedTableInterface<T> table;
    private static final int DEFAULT_LIMIT = 100;
    private final int limit;

    private long fromDbId;
    private long toDbId;
    private List<T> dbEntities;
    private int currentElement = 0;

    public DbTableLoadingIterator(DerivedTableInterface<T> table, int limit, int height) {
        this.table = table;
        this.limit = limit;
        MinMaxValue minMaxValue = table.getMinMaxValue(height);
        this.toDbId = minMaxValue.getMax().longValueExact();
        this.fromDbId = minMaxValue.getMin().longValueExact();
        this.dbEntities = getNextEntities();
    }

    public DbTableLoadingIterator(DerivedTableInterface<T> table, int height) {
        this(table, DEFAULT_LIMIT, height);
    }

    @Override
    public boolean hasNext() {
        if (currentElement < dbEntities.size()) {
            return true;
        }
        if (dbEntities.size() != limit) {
            return false;
        }
        dbEntities = getNextEntities();
        if (dbEntities.size() > 0) {
            currentElement = 0;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public T next() {
        T value = dbEntities.get(currentElement++);
        fromDbId = value.getDbId() + 1;
        return value;
    }

    private List<T> getNextEntities() {
        try {
            return table.getAllByDbId(fromDbId, limit, toDbId).getValues();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to dump db data for table " + table.getName() + ": " + e.toString(), e);
        }
    }
}
