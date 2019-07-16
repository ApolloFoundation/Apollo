/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.util.LockUtils;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * In memory repository for blockchain immutable entities,
 * Main purpose: to store final derived entities and maintain data consistency according to blockchain events
 * Supports get, rollback, insert operations
 *
 * @param <T> derived entity to store
 */
public class InMemoryDerivedEntityRepository<T extends DerivedEntity> {
    private Map<DbKey, T> allEntities = new HashMap<>();
    private long counter = 0;
    private ReadWriteLock lock = new ReentrantReadWriteUpdateLock();

    private KeyFactory<T> keyFactory;

    protected KeyFactory<T> getKeyFactory() {
        return keyFactory;
    }

    public InMemoryDerivedEntityRepository(KeyFactory<T> keyFactory) {
        this.keyFactory = keyFactory;
    }

    protected Map<DbKey, T> getAllEntities() { // should never used directly by client code
        return allEntities;
    }

    private void inWriteLock(Runnable action) {
        LockUtils.doInLock(lock.writeLock(), action);
    }

    private <V> V inReadLock(Supplier<V> action) {
        return LockUtils.getInLock(lock.readLock(), action);
    }

    public void putAll(List<T> objects) {
        inWriteLock(() -> {
            allEntities.putAll(objects.stream()
                    .collect(Collectors.toMap(keyFactory::newKey, Function.identity())));
            Optional<Long> maxId = objects.stream().map(DerivedEntity::getDbId).max(Comparator.naturalOrder());
            counter = maxId.orElse(0L);
        });
    }

    public void clear() {
        inWriteLock(() -> allEntities.clear());
    }

    public void rollback(int height) {
        inWriteLock(() -> {
            List<DbKey> toRollBack = allEntities.entrySet().stream().filter(v -> v.getValue().getHeight() > height).map(Map.Entry::getKey).collect(Collectors.toList());
            toRollBack.forEach(allEntities::remove);
        });
    }

    public T get(DbKey dbKey) {
        return inReadLock(() -> allEntities.get(dbKey));
    }

    public T getCopy(DbKey dbKey) {
        return inReadLock(() -> {

            T existingEntity = allEntities.get(dbKey);
            if (existingEntity != null) {
                try {
                    return (T) existingEntity.clone();
                }
                catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            } else {
                return null;
            }
        });
    }

    public void insert(T entity) {
        inWriteLock(() -> {
            DbKey dbKey = keyFactory.newKey(entity);
            T existingEntity = allEntities.get(dbKey);
            if (existingEntity != null && existingEntity.getHeight() != entity.getHeight()) {
                throw new IllegalArgumentException("Unable to save already existing value");
            }
            allEntities.put(dbKey, entity);
            entity.setDbId(++counter);
        });
    }


    public List<T> getAll(int from, int to) {
        return inReadLock(()-> allEntities.values()
                .stream()
                .sorted(Comparator.comparing(DerivedEntity::getHeight).reversed().thenComparing(Comparator.comparing(DerivedEntity::getDbId).reversed()))
                .skip(from)
                .limit(to - from)
                .collect(Collectors.toList()));
    }

}
