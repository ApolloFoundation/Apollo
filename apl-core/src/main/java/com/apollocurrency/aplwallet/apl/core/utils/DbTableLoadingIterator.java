/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class DbTableLoadingIterator<T extends DerivedEntity> implements Iterator<T> {
    private final DerivedTableInterface<T> table;
    private static final int DEFAULT_LIMIT = 100;
    private final int limit;

    private long fromDbId = -1L;
    private List<T> dbEntities;
    private int currentElement = 0;

    public DbTableLoadingIterator(DerivedTableInterface<T> table, int limit) {
        this.table = table;
        this.limit = limit;
        this.dbEntities = getNextEntities();
    }

    public DbTableLoadingIterator(DerivedTableInterface<T> table) {
        this(table, DEFAULT_LIMIT);
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
        fromDbId = value.getDbId();
        return value;
    }

    private List<T> getNextEntities() {
        try {
            return table.getAllByDbId(fromDbId + 1, limit, Long.MAX_VALUE).getValues();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to dump db data for table " + table.getName() + ": " + e.toString(), e);
        }
    }
}
