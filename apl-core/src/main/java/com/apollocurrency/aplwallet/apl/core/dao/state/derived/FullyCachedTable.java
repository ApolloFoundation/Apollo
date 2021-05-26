/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FullyCachedTable<T extends VersionedDeletableEntity> extends DbTableWrapper<T> {
    private static final int ERROR_DUMP_COUNT = 20_000;


    protected final InMemoryVersionedDerivedEntityRepository<T> memTableCache;

    public FullyCachedTable(InMemoryVersionedDerivedEntityRepository<T> memTableCache, EntityDbTableInterface<T> table) {
        super(table);
        this.memTableCache = memTableCache;
    }

    @Override
    public void insert(T entity) {
        super.insert(entity);
        log.trace("Put into cache {} put  entity {} height={}", table.toString(), entity, entity.getHeight());
        memTableCache.insert(entity);
    }

    @Override
    public T get(DbKey dbKey) {
        return memTableCache.get(dbKey);
    }

    @Override
    public int rollback(final int height) {
        int rc = super.rollback(height);
        memTableCache.rollback(height);
        int dbRowCount = super.getRowCount();
        int memRowCount = memTableCache.rowCount();
        if (dbRowCount != memRowCount) {
            throw new IllegalStateException(String.format("After rollback of the mem and db table %s to the height %d, " +
                    "mem and desync occurred, db rows %d, mem rows %d, db entities: %s, mem entities %s",
                table.toString(), height, dbRowCount, memRowCount, dumpFromDb(), dumpFromMem()));
        }
        return rc;
    }

    @Override
    public boolean deleteAtHeight(T t, int height) {
        boolean res = super.deleteAtHeight(t, height);
        boolean memDeleted = memTableCache.delete(t);
        if (res != memDeleted) {
            throw new IllegalStateException(
                String.format("Desync of in-memory cache and the db, for the table %s after deletion of entity %s " +
                        "at height %d, db entities: %s, mem entities: %s"
                    , table.toString(), t, height, dumpFromDb(), dumpFromMem()));
        }
        if (res) {
            log.trace("Entity type {}, deleted {} from mem table cache and from db, at height {}", table.toString(), t, height);
        }
        return res;
    }

    private String dumpFromDb() {
        try (Stream<T> memRowsStream = memTableCache.getAllRowsStream(0, ERROR_DUMP_COUNT)) {
            OptionalLong minDbId = memRowsStream.mapToLong(DerivedEntity::getDbId).min();
            List<T> dbEntities = super.getAllByDbId(minDbId.orElse(0), Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            return dbEntities.stream().sorted(Comparator.comparingLong(DerivedEntity::getDbId).reversed()).map(Object::toString).collect(Collectors.joining(","));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to dump db data for table " + table + ": " + e.toString(), e);
        }
    }

    private String dumpFromMem() {
        return memTableCache.getAllRowsStream(0, ERROR_DUMP_COUNT).map(Object::toString).collect(Collectors.joining(","));
    }

    @Override
    public void truncate() {
        super.truncate();
        memTableCache.clear();
        log.trace("Mem table {} and db table truncated", table.toString());
    }
}