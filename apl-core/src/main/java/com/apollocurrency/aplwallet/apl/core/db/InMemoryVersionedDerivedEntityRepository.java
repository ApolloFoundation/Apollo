/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * In memory repository for blockchain changeable entities,
 * Main purpose: to store versioned derived entities and maintain data consistency according to blockchain events
 * Supports rollback, trim, delete, insert operations
 *
 * @param <T> versioned derived entity to store
 */
public abstract class InMemoryVersionedDerivedEntityRepository<T extends VersionedDerivedEntity> {

    private Map<DbKey, EntityWithChanges<T>> allEntities = new HashMap<>();
    private long counter = 0;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private KeyFactory<T> keyFactory;
    private List<String> chageableColumns;

    public InMemoryVersionedDerivedEntityRepository(KeyFactory<T> keyFactory, List<String> changeableColumns) {
        this.keyFactory = keyFactory;
        this.chageableColumns = changeableColumns;
    }

    protected KeyFactory<T> getKeyFactory() {
        return keyFactory;
    }

    protected Map<DbKey, EntityWithChanges<T>> getAllEntities() {
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
            Map<DbKey, List<T>> groupedObjects = objects.stream()
                    .collect(groupingBy(keyFactory::newKey,
                            Collectors.collectingAndThen(Collectors.toList(), l -> l.stream()
                                    .sorted(Comparator.comparing(DerivedEntity::getHeight))
                                    .collect(Collectors.toList()))));
            for (Map.Entry<DbKey, List<T>> entry : groupedObjects.entrySet()) {
                DbKey dbKey = entry.getKey();
                List<T> historicalEntities = entry.getValue();
                T lastValue = historicalEntities.get(historicalEntities.size() - 1);
                Map<String, List<Change>> changes = chageableColumns.stream().collect(toMap(Function.identity(), s-> new ArrayList<>()));
                for (T historicalEntity : historicalEntities) {
                    chageableColumns.forEach(name-> {
                        List<Change> columnChanges = changes.get(name);
                        Object columnChange = analyzeChanges(name, columnChanges.size() == 0 ? null : columnChanges.get(columnChanges.size() - 1), historicalEntity);
                        if (columnChange != null) {
                            columnChanges.add(new Change(historicalEntity.getHeight(), columnChange));
                        }
                    });
                }
                EntityWithChanges<T> entityWithChanges = new EntityWithChanges<>(lastValue, changes);
                allEntities.put(dbKey, entityWithChanges);
            }

            Optional<Long> maxId = objects.stream().map(DerivedEntity::getDbId).max(Comparator.naturalOrder());
            counter = maxId.orElse(0L);
        });
    }

    // alternatively, we could use reflection, but it will be slower significantly
    public abstract Object analyzeChanges(String columnName, Object prevValue, T entity);

    // set value for column with such name for entity
    public abstract void setColumn(String columnName, Object value, T entity);

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
            EntityWithChanges<T> entity = allEntities.get(dbKey);
            if (entity != null) {
                T t = entity.getEntity();
                if (t.isLatest()) {
                    return t;
                }
            }
            return null;
        });
    }

    public T getCopy(DbKey dbKey) {
        return inReadLock(() -> {
            EntityWithChanges<T> entity = allEntities.get(dbKey);
            if (entity != null) {
                T lastObject = entity.getEntity();
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
            EntityWithChanges<T> existingEntity = allEntities.get(dbKey);
            entity.setLatest(false);
            if (existingEntity != null) {
                T ourEntity = existingEntity.getEntity();
                ourEntity.setLatest(false);
                List<Change> latestChanges = existingEntity.getChanges().get("latest");
                latestChanges.add(new Change(entity.getHeight(), false));
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

