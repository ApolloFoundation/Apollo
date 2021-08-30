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
            log.debug("{} PUT dbKey: {}, height: {}, entity: {}", tableLogHeader(), entity.getDbKey(), entity.getHeight(), entity);
            cache.put(entity.getDbKey(), (T) entity.deepCopy());
        }
    }

    @Override
    public T get(DbKey dbKey) {
        T t = cache.getIfPresent(dbKey);
        if (t == null) {
            t = super.get(dbKey);
            // do not try to put db value in the cache since it may be inconsistent because of db transaction isolation,
            // you may read not updated value here, but its updated version was committed outside the class (somewhere in BlockchainProcessorImpl) after your read
            // Possible workaround: attach cache updates to the db transaction commit/rollback
        }
        return t == null ? null : (T) t.deepCopy();
    }

    @Override
    public int rollback(final int height) {
        int rc;
        List<T> removedEntities = new ArrayList<>();
        synchronized (lock) {
            rc = super.rollback(height);
            final Map<DbKey, T> map = cache.asMap();
            map.forEach((key, value) -> {
                if (value.getHeight() > height) {
                    cache.invalidate(key);
                    removedEntities.add(value);
                }
            });
        }
        if (!removedEntities.isEmpty()) {
            log.debug("{} ROLLBACK height: {}, from the cache: {}, from the db: {}, removed cache entities: {}", tableLogHeader(), height
                , removedEntities.size(), rc, removedEntities);
        }
        return rc;
    }

    @Override
    public boolean deleteAtHeight(T t, int height) {
        synchronized (lock) {
            boolean rc = super.deleteAtHeight(t, height);
            if (rc) {
                log.debug("{} DELETE  dbKey={} height={} entity {}", tableLogHeader(), t.getDbKey(), t.getHeight(), t);
                cache.invalidate(t.getDbKey());
            }
            return rc;
        }
    }

    @Override
    public void truncate() {
        synchronized (lock) {
            super.truncate();
            log.debug("{} CLEAR ALL", tableLogHeader());
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
