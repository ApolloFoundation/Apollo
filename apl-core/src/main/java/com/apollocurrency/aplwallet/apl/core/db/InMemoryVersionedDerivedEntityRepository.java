/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.model.DbIdLatestValue;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.model.EntityWithChanges;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.util.LockUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        this.keyFactory = Objects.requireNonNull(keyFactory);
        this.chageableColumns = Objects.requireNonNull(changeableColumns);
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

    protected <V> V getInWriteLock(Supplier<V> action) {
        return LockUtils.getInLock(lock.writeLock(), action);
    }

    protected <V> V getInReadLock(Supplier<V> action) {
        return LockUtils.getInLock(lock.readLock(), action);
    }

    protected void inReadLock(Runnable action) {
        LockUtils.doInLock(lock.readLock(), action);
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
                T lastValue = last(historicalEntities);
                Map<String, List<Change>> changes = chageableColumns.stream().collect(toMap(Function.identity(), s -> new ArrayList<>()));
                List<DbIdLatestValue> dbIdLatestValues = new ArrayList<>();
                for (T historicalEntity : historicalEntities) {
                    dbIdLatestValues.add(new DbIdLatestValue(historicalEntity.getHeight(), historicalEntity.isLatest(), historicalEntity.getDbId()));
                    chageableColumns.forEach(name -> {
                        List<Change> columnChanges = changes.get(name);
                        Object columnChange = analyzeChanges(name, getPrevValue(columnChanges), historicalEntity);
                        if (columnChange != null) {
                            columnChanges.add(new Change(historicalEntity.getHeight(), columnChange));
                        }
                    });
                }
                EntityWithChanges<T> entityWithChanges = new EntityWithChanges<>(lastValue, changes, dbIdLatestValues, historicalEntities.get(0).getHeight());
                allEntities.put(dbKey, entityWithChanges);
            }

            Optional<Long> maxId = objects.stream().map(DerivedEntity::getDbId).max(Comparator.naturalOrder());
            counter = maxId.orElse(0L);
        });
    }

    private Object getPrevValue(List<Change> changes) {
        return changes.size() == 0 ? null : last(changes).getValue();
    }

    /**
     * Analyze changes for column with specified {@code columnName} using {@code prevValue} on new {@code entity}.
     * Should return null, if column value was not changed.
     * Should return new value for column in specified entity when prevValue differs from current value.
     * <p>
     *     Note, that alternatively, we could use reflection, but it will be significantly slower
     * </p>
     * @param columnName name of the column to analyze changes
     * @param prevValue previous value of this column (can be null)
     * @param entity new entity, which may contain change for specified column
     * @return new value for column or null, when column value was not changed
     */
    public abstract Object analyzeChanges(String columnName, Object prevValue, T entity);

    // set value for column with such name for entity
    public abstract void setColumn(String columnName, Object value, T entity);

    public void clear() {
        inWriteLock(() -> allEntities.clear());
    }

    public void insert(T entity) {
        inWriteLock(() -> {
            entity.setLatest(true);
            DbKey dbKey = keyFactory.newKey(entity);
            EntityWithChanges<T> existingEntity = allEntities.get(dbKey);
            if (existingEntity == null) { // save new
                entity.setDbId(++counter);
                Map<String, List<Change>> changes = chageableColumns.stream().collect(toMap(Function.identity(), e -> new ArrayList<>()));
                chageableColumns.forEach(c -> {
                    Object change = analyzeChanges(c, null, entity);
                    changes.get(c).add(new Change(entity.getHeight(), change));
                });
                List<DbIdLatestValue> dbIdLatestValues = new ArrayList<>();
                dbIdLatestValues.add(new DbIdLatestValue(entity.getHeight(), true, entity.getDbId()));
                allEntities.put(dbKey, new EntityWithChanges<>(entity, changes, dbIdLatestValues, entity.getHeight()));
            } else {
                if (existingEntity.getEntity().getHeight() == entity.getHeight()) { // do merge
                    Map<String, List<Change>> changes = existingEntity.getChanges();
                    chageableColumns.forEach(c -> {
                        List<Change> columnChanges = changes.get(c);
                        Change lastChange = last(columnChanges);
                        Object change = analyzeChanges(c, lastChange.getValue(), entity);
                        if (change != null || lastChange.getValue() != null) { // change was performed
                            if (lastChange.getHeight() == entity.getHeight()) {
                                lastChange.setValue(change);
                            } else {
                                columnChanges.add(new Change(entity.getHeight(), change));
                            }
                        }
                    });
                    existingEntity.setEntity(entity);
                } else { // do insert new value
                    entity.setDbId(++counter);
                    Map<String, List<Change>> changes = existingEntity.getChanges();
                    chageableColumns.forEach(c -> {
                        List<Change> columnChanges = changes.get(c);
                        Object prevValue = getPrevValue(columnChanges);
                        Object change = analyzeChanges(c, prevValue, entity);
                        if (change != null || prevValue != null) { // new value exists or equal to null
                            columnChanges.add(new Change(entity.getHeight(), change));
                        }
                    });
                    List<DbIdLatestValue> dbIdLatestValues = existingEntity.getDbIdLatestValues();
                    last(dbIdLatestValues).setLatest(false);
                    dbIdLatestValues.add(new DbIdLatestValue(entity.getHeight(), true, entity.getDbId()));
                    existingEntity.setEntity(entity);
                }
            }
        });
    }

    private <V> V last(List<V> l) {
        return l.get(l.size() - 1);
    }

    public T get(DbKey dbKey) {
        return getInReadLock(() -> {
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
        return getInReadLock(() -> {
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
            if (existingEntity != null) {
                entity.setLatest(false);
                T ourEntity = existingEntity.getEntity();
                ourEntity.setLatest(false);
                ourEntity.setHeight(entity.getHeight());
                ourEntity.setDbId(++counter);
                List<DbIdLatestValue> dbIdLatestValues = existingEntity.getDbIdLatestValues();
                last(dbIdLatestValues).setLatest(false);
                dbIdLatestValues.add(new DbIdLatestValue(entity.getHeight(), false, counter));
                return true;
            } else {
                return false;
            }
        });
    }

    public void trim(int height) {
        inReadLock(() -> {
            Set<DbKey> deleteEntirely = new HashSet<>();
            allEntities.forEach((key, l) -> {
                if (l.getMinHeight() < height) {
                    long trimCandidates = l.getDbIdLatestValues().stream().filter(s -> s.getHeight() < height).count();
                    int maxHeight = l.getDbIdLatestValues().stream().filter(s -> s.getHeight() < height).sorted(Comparator.comparing(DbIdLatestValue::getHeight).reversed()).map(DbIdLatestValue::getHeight).findFirst().orElse(0);
                    if (trimCandidates > 1) {
                        Map<String, List<Change>> allChanges = l.getChanges();
                        for (Map.Entry<String, List<Change>> columnWithChanges : allChanges.entrySet()) {
                            List<Change> changes = columnWithChanges.getValue();
                            changes.removeIf(c -> c.getHeight() < maxHeight);
                        }
                        l.getDbIdLatestValues().removeIf(s -> s.getHeight() < maxHeight);
                    }
                    boolean delete = l.getDbIdLatestValues().stream().noneMatch(s -> s.getHeight() >= height) && !l.getEntity().isLatest();

                    if (delete) {
                        deleteEntirely.add(key);
                    }
                }
            });
            deleteEntirely.forEach(key -> allEntities.remove(key));
        });
    }

    public void rollback(int height) {
        inWriteLock(() -> {
            Set<DbKey> keysToUpdate = new HashSet<>();
            Set<DbKey> keysToDelete = new HashSet<>();
            for (Map.Entry<DbKey, EntityWithChanges<T>> entry : allEntities.entrySet()) {
                EntityWithChanges<T> entity = entry.getValue();
                DbKey key = entry.getKey();
                if (entity.getEntity().getHeight() > height) {
                    if (entity.getMinHeight() > height) {
                        keysToDelete.add(key);
                    } else {
                        keysToUpdate.add(key);
                        Map<String, List<Change>> allChanges = entity.getChanges();
                        for (Map.Entry<String, List<Change>> columnWithChanges : allChanges.entrySet()) {
                            List<Change> changes = columnWithChanges.getValue();
                            boolean removed = changes.removeIf(c -> c.getHeight() > height);
                            if (removed) {
                                setColumn(columnWithChanges.getKey(), last(changes).getValue(), entity.getEntity());
                            }
                        }
                        entity.getDbIdLatestValues().removeIf(l -> l.getHeight() > height);
                    }
                }
            }
            keysToDelete.forEach(allEntities::remove);
            keysToUpdate.stream()
                    .map(allEntities::get)
                    .forEach(e -> {
                        T entity = e.getEntity();
                        entity.setLatest(true);
                        List<DbIdLatestValue> dbIdLatestValues = e.getDbIdLatestValues();
                        DbIdLatestValue lastDbIdLatestValue = last(dbIdLatestValues);
                        entity.setDbId(lastDbIdLatestValue.getDbId());
                        entity.setHeight(lastDbIdLatestValue.getHeight());
                        lastDbIdLatestValue.setLatest(true);
                    });
        });
    }

    public List<T> getAll(Comparator<T> comparator, int from, int to) {
        return getInReadLock(() ->
                CollectionUtil.limitStream(
                        allEntities.values()
                                .stream()
                                .map(EntityWithChanges::getEntity)
                                .filter(T::isLatest)
                                .sorted(comparator)
                        , from, to)
                        .collect(Collectors.toList()));
    }
}

