/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.utils.DbTableLoadingIterator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;
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
            new RowsConsistentOperationExecutor(height, "trim").doOp(() -> {
                super.trim(height);
                log.info("Trim in memory table {} at height {}", getName(), height);
                memTableCache.trim(height);
            });
        }
    }

    @Override
    public T get(DbKey dbKey) {
        return memTableCache.get(dbKey);
    }

    @Override
    public int rollback(final int height) {
        synchronized (lock) {
            return new RowsConsistentOperationExecutor(height, "rollback").doOp(()-> {
                int rc = super.rollback(height);
                memTableCache.rollback(height);
                return rc;
            });
        }
    }

    @Override
    public boolean deleteAtHeight(T t, int height) {
        synchronized (lock) {
            return new RowsConsistentOperationExecutor(height, "deleteAtHeight").doOp(() -> {
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
            });

        }
    }

    @Override
    public void truncate() {
        synchronized (lock) {
            new RowsConsistentOperationExecutor(0, "truncate").doOp(() -> {
                super.truncate();
                memTableCache.clear();
                log.info("Mem table {} and db table truncated", getName());
            });
        }
    }

    private void findInconsistency(int height) {
        try (Stream<T> memRowsStream = FullyCachedTable.this.memTableCache.getAllRowsStream(0, -1)) {
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


    @AllArgsConstructor
    protected class RowsConsistentOperationExecutor {
        private final int height;
        private final String opName;

        protected <V> V doOp(Supplier<V> op) {
            RowsCount beforeOpRowsCount = new RowsCount(height);
            V result = op.get();
            RowsCount afterOpRowsCount = new RowsCount(height);
            afterOpRowsCount.verifyAgainst(beforeOpRowsCount, opName);
            return result;
        }

        protected void doOp(Runnable op) {
            doOp(() -> {
                op.run();
                return null;
            });
        }
    }

    protected class RowsCount {
        private final int db;
        private final int mem;
        private final int height;

        public RowsCount(int height) {
            MinMaxValue minMaxValue = FullyCachedTable.this.getMinMaxValue(height);
            this.db = (int) minMaxValue.getCount();
            this.mem = memTableCache.rowCount(height);
            this.height = height;
        }

        protected boolean isConsistent() {
            return db == mem;
        }

        protected void verifyAgainst(RowsCount beforeOpRowsCount, String operation) {
            if (beforeOpRowsCount.isConsistent()) {
                if (!isConsistent()) {
                    FullyCachedTable.this.findInconsistency(height);
                    throw new IllegalStateException(createErrorMessage(operation));
                }
            } else {
                log.warn("Before {} operation for table {} at height {}, db and mem tables are inconsistent by rows, db {}, mem {}. " +
                        "Maybe this db transaction was started before changes to the mem table applied. Will not verify consistency after this operation",
                    operation, height, FullyCachedTable.this.getName(), db, mem);
            }
        }

        private String createErrorMessage(String operation) {
            return String.format("After %s of the mem and db table %s to the height %d, " +
                    "mem and db desync occurred, db rows %d, mem rows %d", operation,
                getName(), height, db, mem);
        }
    }
}