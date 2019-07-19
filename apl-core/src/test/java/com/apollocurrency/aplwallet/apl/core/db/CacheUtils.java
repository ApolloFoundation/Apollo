/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.apollocurrency.aplwallet.apl.core.db.derived.BasicDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;

import java.util.List;
import java.util.Map;

public class CacheUtils<T> {

    private BasicDbTable<T> table;
    private DbExtension extension;

    public CacheUtils(BasicDbTable<T> table, DbExtension extension) {
        this.table = table;
        this.extension = extension;
    }

    public void assertInCache(T value) {
        T cachedValue = (T) getCache(table.getDbKeyFactory().newKey(value));
        assertEquals(value, cachedValue);
    }

    public void assertNotInCache(T value) {
        DerivedEntity cachedValue = getCache(table.getDbKeyFactory().newKey(value));
        assertNotEquals(value, cachedValue);
    }

    public void assertListNotInCache(List<T> values) {
        values.forEach(this::assertNotInCache);
    }

    public void assertListInCache(List<T> values) {
        values.forEach(this::assertInCache);
    }

    public DerivedEntity getCache(DbKey dbKey) {
        if (!extension.getDatabaseManager().getDataSource().isInTransaction()) {
            return DbUtils.getInTransaction(extension, (con) -> getCacheInTransaction(dbKey));
        } else {
            return getCacheInTransaction(dbKey);
        }
    }

    public DerivedEntity getCacheInTransaction(DbKey dbKey) {
        Map<DbKey, Object> cache = extension.getDatabaseManager().getDataSource().getCache(table.getTableName());
        return (DerivedEntity) cache.get(dbKey);
    }


    public void removeFromCache(T value) {
        DbKey dbKey = table.getDbKeyFactory().newKey(value);
        Map<DbKey, Object> cache = extension.getDatabaseManager().getDataSource().getCache(table.getTableName());
        cache.remove(dbKey);
    }

    public void assertInCache(List<T> values) {
        List<T> cachedValues = getCacheList(table.getDbKeyFactory().newKey(values.get(0)));
        assertEquals(values, cachedValues);
    }

    public void assertNotInCache(List<T> values) {
        List<T> cachedValues = getCacheList(table.getDbKeyFactory().newKey(values.get(0)));
        assertNotEquals(values, cachedValues);
    }

    public List<T> getCacheList(DbKey dbKey) {
        if (!extension.getDatabaseManager().getDataSource().isInTransaction()) {
            return DbUtils.getInTransaction(extension, (con) -> getCacheListInTransaction(dbKey));
        } else {
            return getCacheListInTransaction(dbKey);
        }
    }

    public List<T> getCacheListInTransaction(DbKey dbKey) {
        Map<DbKey, Object> cache = extension.getDatabaseManager().getDataSource().getCache(table.getTableName());
        return (List<T>) cache.get(dbKey);
    }


    public void removeFromCache(List<T> values) {
        DbKey dbKey =table.getDbKeyFactory().newKey(values.get(0));
        Map<DbKey, Object> cache = extension.getDatabaseManager().getDataSource().getCache(table.getTableName());
        cache.remove(dbKey);
    }
}
