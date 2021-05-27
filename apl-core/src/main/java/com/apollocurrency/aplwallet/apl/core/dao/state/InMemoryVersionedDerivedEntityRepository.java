/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.Change;
import com.apollocurrency.aplwallet.apl.core.db.model.DbIdLatestValue;
import com.apollocurrency.aplwallet.apl.core.db.model.EntityWithChanges;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.LockUtils;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * In memory repository for blockchain changeable entities,
 * Main purpose: to store versioned derived entities and maintain data consistency according to blockchain events
 * Supports rollback, trim, delete, insert operations
 *
 * @param <T> versioned derived entity to store
 */
@Slf4j
public abstract class InMemoryVersionedDerivedEntityRepository<T extends VersionedDeletableEntity> {

    private final Map<DbKey, EntityWithChanges<T>> allEntities = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final KeyFactory<T> keyFactory;
    private final List<String> changeableColumns;
    private int rows = 0;

    public InMemoryVersionedDerivedEntityRepository(KeyFactory<T> keyFactory, List<String> changeableColumns) {
        this.keyFactory = Objects.requireNonNull(keyFactory);
        this.changeableColumns = Objects.requireNonNull(changeableColumns);
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
                Map<String, List<Change>> changes = changeableColumns.stream().collect(toMap(Function.identity(), s -> new ArrayList<>()));
                List<DbIdLatestValue> dbIdLatestValues = new ArrayList<>();
                for (T historicalEntity : historicalEntities) {
                    dbIdLatestValues.add(new DbIdLatestValue(historicalEntity.getHeight(), historicalEntity.isLatest(), historicalEntity.isDeleted(), historicalEntity.getDbId()));
                    changeableColumns.forEach(name -> {
                        List<Change> columnChanges = changes.get(name);
                        Object prevValue = getLastValueOrNull(columnChanges);
                        Value columnChange = analyzeChanges(name, prevValue, historicalEntity);
                        if (columnChange.isChanged()) {
                            columnChanges.add(new Change(historicalEntity.getHeight(), columnChange.getV()));
                        }
                    });
                }
                EntityWithChanges<T> entityWithChanges = new EntityWithChanges<>(lastValue, changes, dbIdLatestValues, historicalEntities.get(0).getHeight());
                allEntities.put(dbKey, entityWithChanges);
            }
            rows += objects.size();
        });
    }

    private Object getLastValueOrNull(List<Change> changes) {
        return changes.size() == 0 ? null : last(changes).getValue();
    }

    /**
     * Analyze changes for column with specified {@code columnName} using {@code prevValue} on new {@code entity}.
     * Should return false for {@link Value#changed}, if column value was not changed.
     * Should return new value for column in specified entity when prevValue differs from current value: true for {@link Value#changed} and new value for {@link Value#v}
     * Should not return null in any case
     * <p>
     *     Note, that alternatively, we could use reflection, but it will be significantly slower
     * </p>
     * @param columnName name of the column to analyze changes
     * @param prevValue previous value of this column (can be null)
     * @param entity new entity, which may contain change for specified column
     * @return new value for column or empty {@link Value}, when column value was not changed
     */
    protected abstract Value analyzeChanges(String columnName, Object prevValue, T entity);

    /**
     * Set value for column with such name for given entity
     * @param columnName name of the column to set
     * @param value value of the column to set, can be null
     * @param entity entity which should be updated using given value and column name
     */
    protected abstract void setColumn(String columnName, Object value, T entity);

    public void clear() {
        inWriteLock(() -> {
            allEntities.clear();
            rows = 0;
        });
    }

    public void insert(T entity) {
        inWriteLock(() -> {
            entity.setLatest(true);
            DbKey dbKey = keyFactory.newKey(entity);
            EntityWithChanges<T> existingEntity = allEntities.get(dbKey);
            if (existingEntity == null) { // save new
                entity.setDbId(entity.getDbId());
                Map<String, List<Change>> changes = changeableColumns.stream().collect(toMap(Function.identity(), e -> new ArrayList<>()));
                changeableColumns.forEach(c -> {
                    Value value = analyzeChanges(c, null, entity);
                    if (value.isChanged()) {
                        changes.get(c).add(new Change(entity.getHeight(), value.getV()));
                    }
                });
                List<DbIdLatestValue> dbIdLatestValues = new ArrayList<>();
                dbIdLatestValues.add(new DbIdLatestValue(entity.getHeight(), true, false, entity.getDbId()));
                allEntities.put(dbKey, new EntityWithChanges<>(entity, changes, dbIdLatestValues, entity.getHeight()));
                rows++;
                log.info("Saved new in-memory entity {}, total entities {}", entity, rows);
            } else {
                if (existingEntity.getEntity().getHeight() == entity.getHeight()) { // do merge
                    Map<String, List<Change>> changes = existingEntity.getChanges();
                    changeableColumns.forEach(c -> {
                        List<Change> columnChanges = changes.get(c);
                        Change lastChange = last(columnChanges);
                        Value value = analyzeChanges(c, lastChange.getValue(), entity);
                        if (value.isChanged()) { // change was performed
                            if (lastChange.getHeight() == entity.getHeight()) {
                                lastChange.setValue(value.getV());
                            } else {
                                columnChanges.add(new Change(entity.getHeight(), value.getV()));
                            }
                        }
                    });
                    existingEntity.setEntity(entity);
                    log.info("Merge in-memory entity {}, total entities {}", entity, rows);
                } else { // do insert new value
                    entity.setDbId(entity.getDbId());
                    Map<String, List<Change>> changes = existingEntity.getChanges();
                    changeableColumns.forEach(c -> {
                        List<Change> columnChanges = changes.get(c);
                        Object prevValue = getLastValueOrNull(columnChanges);
                        Value value = analyzeChanges(c, prevValue, entity);
                        if (value.isChanged()) { // new value exists or equal to null
                            columnChanges.add(new Change(entity.getHeight(), value.getV()));
                        }
                    });
                    List<DbIdLatestValue> dbIdLatestValues = existingEntity.getDbIdLatestValues();
                    last(dbIdLatestValues).setLatest(false);
                    dbIdLatestValues.add(new DbIdLatestValue(entity.getHeight(), true, false, entity.getDbId()));
                    existingEntity.setEntity(entity);
                    rows++;
                    log.info("Updated in-memory entity {}, total entities {}", entity, rows);
                }
            }
        });
    }

    private <V> V last(List<V> l) {
        return l.get(l.size() - 1);
    }

    public T get(DbKey dbKey) {
        return getCopy(dbKey);
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
                ourEntity.setDbId(entity.getDbId());
                ourEntity.setDeleted(true);
                List<DbIdLatestValue> dbIdLatestValues = existingEntity.getDbIdLatestValues();
                last(dbIdLatestValues).setLatest(false);
                last(dbIdLatestValues).setDeleted(true);
                dbIdLatestValues.add(new DbIdLatestValue(entity.getHeight(), false, true, entity.getDbId()));
                rows++;
                log.info("Deleted entity {} at height {}", entity, entity.getHeight());
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
                        int prevRows = l.getDbIdLatestValues().size();
                        l.getDbIdLatestValues().removeIf(s -> s.getHeight() < maxHeight);
                        rows -= (prevRows - l.getDbIdLatestValues().size());
                    }
                    boolean delete = l.getDbIdLatestValues().stream().noneMatch(s -> s.getHeight() >= height) && !l.getEntity().isLatest();

                    if (delete) {
                        deleteEntirely.add(key);
                        rows -= l.getDbIdLatestValues().size();
                    }
                }
            });
            deleteEntirely.forEach(allEntities::remove);
        });
    }

    public int rollback(int height) {
        return getInWriteLock(() -> {
            int removedRecords = 0;
            Set<DbKey> keysToUpdate = new HashSet<>();
            Set<DbKey> keysToDelete = new HashSet<>();
            Set<DbKey> keysToRenewDeleted = new HashSet<>();
            for (Map.Entry<DbKey, EntityWithChanges<T>> entry : allEntities.entrySet()) {
                EntityWithChanges<T> entity = entry.getValue();
                DbKey key = entry.getKey();
                if (entity.getEntity().getHeight() > height) {
                    if (entity.getMinHeight() > height) {
                        keysToDelete.add(key);
                        removedRecords += entity.getDbIdLatestValues().size();
                    } else {
                        keysToUpdate.add(key);
                        Map<String, List<Change>> allChanges = entity.getChanges();
                        for (Map.Entry<String, List<Change>> columnWithChanges : allChanges.entrySet()) {
                            List<Change> changes = columnWithChanges.getValue();
                            boolean removed = changes.removeIf(c -> c.getHeight() > height);
                            if (removed) {
                                setColumn(columnWithChanges.getKey(), getLastValueOrNull(changes), entity.getEntity());
                            }
                        }
                        List<DbIdLatestValue> dbIdLatestValues = entity.getDbIdLatestValues();
                        int initialSize = dbIdLatestValues.size();
                        long deletedEntriesCount = dbIdLatestValues.stream().filter(l -> l.getHeight() > height && l.isDeleted()).count();
                        if (deletedEntriesCount % 2 != 0) {
                            keysToRenewDeleted.add(key);
                        }
                        dbIdLatestValues.removeIf(l -> l.getHeight() > height);

                        removedRecords += (initialSize - dbIdLatestValues.size());
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
            keysToRenewDeleted.stream().map(allEntities::get).forEach(e-> {
                T entity = e.getEntity();
                entity.setDeleted(false);
                last(e.getDbIdLatestValues()).setDeleted(false);
            });
            rows -= removedRecords;
            return removedRecords;
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

    public int rowCount() {
        return getInReadLock(()-> rows);
    }

    public Stream<T> getAllRowsStream(int from, int to) {
        return getInReadLock(() ->
            CollectionUtil.limitStream(allEntities.values()
                .stream()
                .flatMap(entityWithChanges->reconstructHistoricalEntries(entityWithChanges).stream())
                .sorted(Comparator.comparingLong(DerivedEntity::getDbId)), from, to));
    }

    private List<T> reconstructHistoricalEntries(EntityWithChanges<T> entityWithChanges) {
        List<T> historicalEntities = new ArrayList<>();
        entityWithChanges.getDbIdLatestValues().forEach(e -> {
            try {
                T historicalEntity = (T) entityWithChanges.getEntity().clone();
                historicalEntity.setDbId(e.getDbId());
                historicalEntity.setHeight(e.getHeight());
                historicalEntity.setLatest(e.isLatest());
                historicalEntity.setDeleted(e.isDeleted());
                changeableColumns.forEach(column -> {
                    Optional<Change> changesForColumnAtHeight = entityWithChanges.getChangesForColumnAtHeight(column, e.getHeight());
                    if (changesForColumnAtHeight.isEmpty()) {
                        return;
                    }
                    setColumn(column, changesForColumnAtHeight.get().getValue(), historicalEntity);
                });
                historicalEntities.add(historicalEntity);
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException("Cloning operation should be supported for the in-memory table entity", ex);
            }
        });
        return historicalEntities;
    }

    @ToString
    public static class Value {
        private boolean changed;
        private Object v;

        public boolean isChanged() {
            return changed;
        }

        public Object getV() {
            return v;
        }

        public Value() {}

        public Value(Object v) {
            this.changed = true;
            this.v = v;
        }
    }
}

