/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class CachedTable<T extends DerivedEntity> extends DbTableWrapper<T> {

    private final Cache<DbKey, T> cache;

    public CachedTable(Cache<DbKey, T> cache, EntityDbTableInterface<T> table) {
        super(table);
        this.cache = cache;
        log.info("--cache-- init PUBLIC KEY CACHE={}", cache.stats());
    }

    @Override
    public void insert(T entity) {
        super.insert(entity);
        log.trace("--cache-- put  dbKey={} height={}", entity.getDbKey(), entity.getHeight());
        cache.put(entity.getDbKey(), entity);
    }

    @Override
    public void rollback(final int height) {
        super.rollback(height);
        final Map<DbKey, T> map = cache.asMap();
        map.values().forEach(v -> {
            if(v.getHeight()>height) {
                log.trace("--cache-- remove  dbKey={} height={}", v.getDbKey(), v.getHeight());
                cache.invalidate(v.getDbKey());
            }
        });
    }

    @Override
    public boolean delete(T t) {
        boolean rc = super.delete(t);
        if (rc){
            log.trace("--cache-- remove  dbKey={} height={}", t.getDbKey(), t.getHeight());
            cache.invalidate(t.getDbKey());
        }
        return rc;
    }

    @Override
    public void truncate() {
        super.truncate();
        log.trace("--cache-- remove  ALL");
        cache.invalidateAll();
    }

    //other methods don't suppose to affect the cache value

}
