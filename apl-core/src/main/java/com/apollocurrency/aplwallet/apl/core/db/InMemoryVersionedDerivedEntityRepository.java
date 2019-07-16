/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static java.util.stream.Collectors.groupingBy;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.util.LockUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * In memory repository for blockchain changeable entities,
 * Main purpose: to store versioned derived entities and maintain data consistency according to blockchain events
 * Supports rollback, trim, delete, insert operations
 *
 * @param <T> versioned derived entity to store
 */
public class InMemoryVersionedDerivedEntityRepository<T extends VersionedDerivedEntity> {

    private Map<DbKey, List<T>> allEntities = new HashMap<>();
    private long counter = 0;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private KeyFactory<T> keyFactory;

    public InMemoryVersionedDerivedEntityRepository(KeyFactory<T> keyFactory) {
        this.keyFactory = keyFactory;
    }

    protected KeyFactory<T> getKeyFactory() {
        return keyFactory;
    }

    protected Map<DbKey, List<T>> getAllEntities() {
        return allEntities;
    }

    protected void inWriteLock(Runnable action) {
        LockUtils.doInLock(lock.writeLock(), action);
    }

    protected  <V> V getInWriteLock(Supplier<V> action) {
        return LockUtils.getInLock(lock.writeLock(), action);
    }

    protected  <V> V inReadLock(Supplier<V> action) {
        return LockUtils.getInLock(lock.readLock(), action);
    }


    public void putAll(List<T> objects) {
        inWriteLock(() ->
        {
            allEntities.putAll(objects.stream()
                    .collect(groupingBy(keyFactory::newKey,
                            Collectors.collectingAndThen(Collectors.toList(), l -> l.stream()
                                    .sorted(Comparator.comparing(DerivedEntity::getHeight))
                                    .collect(Collectors.toList())))));
            Optional<Long> maxId = objects.stream().map(DerivedEntity::getDbId).max(Comparator.naturalOrder());
            counter = maxId.orElse(0L);
        });
    }

    public void clear() {
        inWriteLock(() -> allEntities.clear());
    }

    public void insert(T entity) {
        inWriteLock(() -> {
            entity.setLatest(true);
            entity.setDbId(++counter);
            DbKey dbKey = keyFactory.newKey(entity);
            List<T> existingEntities = allEntities.get(dbKey);
            if (existingEntities == null) {
                List<T> entities = new ArrayList<>();
                entities.add(entity);
                allEntities.put(dbKey, entities);
            } else {
                int lastPosition = existingEntities.size() - 1; // assume that existing list of entities has min size 1
                T lastEntity = existingEntities.get(lastPosition);
                if (lastEntity.getHeight() == entity.getHeight()) {
                    existingEntities.set(lastPosition, entity); // do merge
                } else { // do insert new value
                    lastEntity.setLatest(false);
                    existingEntities.add(entity);
                }
            }
        });
    }

    public T get(DbKey dbKey) {
        return inReadLock(() -> {
            List<T> entities = allEntities.get(dbKey);
            if (entities != null) {
                T t = entities.get(entities.size() - 1);
                if (t.isLatest()) {
                    return t;
                }
            }
            return null;
        });
    }

    public T getCopy(DbKey dbKey) {
        return inReadLock(() -> {
            List<T> existingEntities = allEntities.get(dbKey);
            if (existingEntities != null) {
                T lastObject = existingEntities.get(existingEntities.size() - 1);
                try {
                    T clone = (T) lastObject.clone();
                    if (clone.isLatest()) {
                        return clone;
                    }
                }
                catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
            return null;
        });
    }

    public boolean delete(T entity) {
        return getInWriteLock(() -> {
            DbKey dbKey = getKeyFactory().newKey(entity);
            List<T> existingEntities = allEntities.get(dbKey);
            entity.setLatest(false);
            if (existingEntities != null) {
                int lastPosition = existingEntities.size() - 1; // assume that existing list of entities has min size 1
                T existingEntity = existingEntities.get(lastPosition);
                existingEntity.setLatest(false);
                existingEntities.add(entity);
                return true;
            } else {
                return false;
            }
        });
    }

    public void trim(int height) { // no lock because we delete old data, which cannot be retrieved before or after trim
        Set<DbKey> deleteEntirely = new HashSet<>();
        allEntities.forEach((key, l) -> {
            List<T> trimCandidates = l.stream().filter(s -> s.getHeight() < height).sorted(Comparator.comparing(VersionedDerivedEntity::getHeight)).collect(Collectors.toList());
            if (trimCandidates.size() > 1) {
                for (int i = 0; i < trimCandidates.size() - 1; i++) {
                    l.remove(trimCandidates.get(i));
                }
            }
            boolean delete = l.stream().noneMatch(s -> s.getHeight() >= height);
            if (delete) {
                List<T> deleteCandidates = l.stream().filter(entity -> entity.getHeight() < height && !entity.isLatest()).sorted(Comparator.comparing(VersionedDerivedEntity::getHeight)).collect(Collectors.toList());
                for (T deleteCandidate : deleteCandidates) {
                    l.remove(deleteCandidate);
                }
            }
            if (l.size() == 0) {
                deleteEntirely.add(key);
            }
        });
        deleteEntirely.forEach(key -> allEntities.remove(key));
    }

    public void rollback(int height) {
        inWriteLock(() -> {
            Set<DbKey> keysToUpdate = allEntities.entrySet()
                    .stream()
                    .filter((e) -> e.getValue()
                            .stream()
                            .anyMatch(v -> v.getHeight() > height))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            allEntities.values().forEach(l -> l.removeIf(s -> s.getHeight() > height));
            List<DbKey> dbKeysToDelete = allEntities.entrySet().stream().filter(e -> e.getValue().size() == 0).map(Map.Entry::getKey).collect(Collectors.toList());
            dbKeysToDelete.forEach(allEntities::remove);
            keysToUpdate.stream()
                    .map(allEntities::get)
                    .forEach(l -> l.get(l.size() - 1).setLatest(true));
        });
    }

    public List<T> getAll(Comparator<T> comparator, int from, int to) {
        return inReadLock(() ->
                CollectionUtil.limitStream(
                        allEntities.values()
                                .stream()
                                .filter(l -> l.get(l.size() - 1).isLatest())
                                .map(l -> l.get(l.size() - 1))
                                .sorted(comparator)
                        , from, to)
                .collect(Collectors.toList()));
    }
}

