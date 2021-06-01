/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CachedTable<T extends DerivedEntity> extends DbTableWrapper<T> {

    private final Cache<DbKey, T> cache;
    private final Object lock = new Object();

    public CachedTable(Cache<DbKey, T> cache, EntityDbTableInterface<T> table) {
        super(table);
        this.cache = cache;
        log.info("{} INIT stats={}", tableLogHeader(), cache.stats());
    }

    @Override
    public void insert(T entity) {
        synchronized (lock) {
            super.insert(entity);
            log.info("{} PUT dbKey: {}, height: {}, entity: {}", tableLogHeader(), entity.getDbKey(), entity.getHeight(), entity);
            cache.put(entity.getDbKey(), entity);
        }
    }

    @Override
    public T get(DbKey dbKey) {
        T t = cache.getIfPresent(dbKey);
        if (t == null) {
            t = super.get(dbKey);
            if (t != null) {
                synchronized (lock) {
                    t = super.get(dbKey);
                    if (t != null) {
                        log.info("{} PUT MISSING FROM THE DB dbKey: {}, height: {}, entity: {}", tableLogHeader(), dbKey, t.getHeight(), t);
                        cache.put(dbKey, t);
                    }
                }
            }
        }
        return t;
    }

    @Override
    public int rollback(final int height) {
        int rc;
        List<T> removedEntities = new ArrayList<>();
        synchronized (lock) {
            rc = super.rollback(height);
            final Map<DbKey, T> map = cache.asMap();
            map.values().forEach(v -> {
                if (v.getHeight() > height) {
                    cache.invalidate(v.getDbKey());
                    removedEntities.add(v);
                }
            });
        }
        if (!removedEntities.isEmpty()) {
            log.info("{} ROLLBACK height: {}, from the cache: {}, from the db: {}, removed cache entities: {}", tableLogHeader(), height
                , removedEntities.size(), rc, removedEntities);
        }
        return rc;
    }

    @Override
    public boolean deleteAtHeight(T t, int height) {
        synchronized (lock) {
            boolean rc = super.deleteAtHeight(t, height);
            if (rc) {
                log.info("{} DELETE  dbKey={} height={} entity {}", tableLogHeader(), t.getDbKey(), t.getHeight(), t);
                cache.invalidate(t.getDbKey());
            }
            return rc;
        }
    }

    @Override
    public void truncate() {
        synchronized (lock) {
            super.truncate();
            log.info("{} CLEAR ALL", tableLogHeader());
            cache.invalidateAll();
        }
    }

    private String tableName() {
        return table.getName().toUpperCase();
    }

    private String tableLogHeader() {
        return "--" + tableName() + " " + "CACHE--";
    }

}
