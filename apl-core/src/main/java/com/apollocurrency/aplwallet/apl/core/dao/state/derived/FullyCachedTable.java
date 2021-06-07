/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.utils.DbTableLoadingIterator;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
public class FullyCachedTable<T extends VersionedDeletableEntity> extends DbTableWrapper<T> {

    protected final InMemoryVersionedDerivedEntityRepository<T> memTableCache;
    private final Object lock = new Object();

    public FullyCachedTable(InMemoryVersionedDerivedEntityRepository<T> memTableCache, EntityDbTableInterface<T> table) {
        super(table);
        this.memTableCache = memTableCache;
    }

    @Override
    public void insert(T entity) {
        synchronized (lock) {
            super.insert(entity);
            memTableCache.insert(entity);
            log.info("Put into cache {} entity {} height={}", getName(), entity, entity.getHeight());
        }
    }

    @Override
    public void trim(int height) {
        synchronized (lock) {
            super.trim(height);
            log.info("Trim in memory table {} at height {}", getName(), height);
            memTableCache.trim(height);
            checkRowConsistency("trim", height);
        }
    }

    @Override
    public T get(DbKey dbKey) {
        return memTableCache.get(dbKey);
    }

    @Override
    public int rollback(final int height) {
        synchronized (lock) {
            int rc = super.rollback(height);
            memTableCache.rollback(height);
            checkRowConsistency("rollback", height);
            return rc;
        }
    }

    @Override
    public boolean deleteAtHeight(T t, int height) {
        synchronized (lock) {
            boolean res = super.deleteAtHeight(t, height);
            boolean memDeleted = memTableCache.delete(t);
            if (res != memDeleted) {
                findInconsistency(height);
                throw new IllegalStateException(
                    String.format("Desync of in-memory cache and the db, for the table %s after deletion of entity %s " +
                            "at height %d"
                        , getName(), t, height));
            }
            if (res) {
                log.info("Entity type {}, deleted {} from mem table cache and from db, at height {}", table.toString(), t, height);
            }
            return res;
        }
    }

    @Override
    public void truncate() {
        synchronized (lock) {
            super.truncate();
            memTableCache.clear();
            log.info("Mem table {} and db table truncated", getName());
        }
    }

    private void findInconsistency(int height) {
        try (Stream<T> memRowsStream = memTableCache.getAllRowsStream(0, -1)) {
            DbTableLoadingIterator<T> dbIterator = new DbTableLoadingIterator<>(table, 100, height);
            memRowsStream.forEach(memEntity -> {
                if (dbIterator.hasNext()) {
                    T dbEntity = dbIterator.next();
                    if (dbEntity.equals(memEntity)) {
                        return;
                    }
                    log.error("Memory entity: {} does not match corresponding db entity: {}", memEntity, dbEntity);
                } else {
                    log.error("Cached entity present in mem, but not in db: {}", memEntity);
                }
            });
            if (dbIterator.hasNext()) {
                log.error("Present in db, but not in in-memory cache: {}", dbIterator.next());
            }
        }
    }

    private void checkRowConsistency(String operation, int height) {
        MinMaxValue minMaxValue = super.getMinMaxValue(height);
        int dbRowCount = (int) minMaxValue.getCount();
        int memRowCount = memTableCache.rowCount(height);
        if (dbRowCount != memRowCount) {
            findInconsistency(height);
            throw new IllegalStateException(createErrorMessage(operation, height, memRowCount, dbRowCount));
        }
    }

    private String createErrorMessage(String operation, int height, long memRowCount, long dbRowCount) {
        return String.format("After %s of the mem and db table %s to the height %d, " +
                "mem and db desync occurred, db rows %d, mem rows %d", operation,
            getName(), height, dbRowCount, memRowCount);
    }
}