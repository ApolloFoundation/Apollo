/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.LockUtils;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toConcurrentMap;
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

    private final Map<DbKey, EntityWithChanges<T>> allEntities = new ConcurrentHashMap<>();
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
                T lastValue = last(historicalEntities).orElseThrow(()-> new IllegalStateException("Required at least one historical entry to get last value, " +
                    "bad grouping operation was performed earlier for the entry: " + entry));
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
        return changes.size() == 0 ? null : last(changes).orElseThrow(
            () -> new IllegalStateException("Unable last changes for the list " + changes.toString() + ", no elements")
        ).getValue();
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
            T entityCopy = copyEntity(entity);
            entityCopy.setLatest(true);
            entityCopy.setDeleted(false);
            DbKey dbKey = keyFactory.newKey(entityCopy);
            EntityWithChanges<T> existingEntity = allEntities.get(dbKey);
            if (existingEntity == null) { // save new
                entityCopy.setDbId(entityCopy.getDbId());
                Map<String, List<Change>> changes = changeableColumns.stream().collect(toConcurrentMap(Function.identity(), e -> new ArrayList<>()));
                changeableColumns.forEach(c -> {
                    Value value = analyzeChanges(c, null, entityCopy);
                    if (value.isChanged()) {
                        changes.get(c).add(new Change(entityCopy.getHeight(), value.getV()));
                    }
                });
                List<DbIdLatestValue> dbIdLatestValues = new ArrayList<>();
                dbIdLatestValues.add(new DbIdLatestValue(entityCopy.getHeight(), true, false, entityCopy.getDbId()));
                allEntities.put(dbKey, new EntityWithChanges<>(entityCopy, changes, dbIdLatestValues, entityCopy.getHeight()));
                rows++;
                log.info("Saved new in-memory entity {}, total entities {}", entityCopy, rows);
            } else {
                updateExisting(entityCopy, existingEntity);
            }
        });
    }

    public T get(DbKey dbKey) {
        return getCopy(dbKey);
    }

    public T getCopy(DbKey dbKey) {
        return getInReadLock(() -> {
            EntityWithChanges<T> entity = allEntities.get(dbKey);
            if (entity != null) {
                T lastObject = entity.getEntity();
                T clone = (T) lastObject.deepCopy();
                if (clone.isLatest()) {
                    return clone;
                }
            }
            return null;
        });
    }

    public boolean delete(T entity) {
        return getInWriteLock(() -> {
            T entityCopy = copyEntity(entity);
            DbKey dbKey = getKeyFactory().newKey(entityCopy);
            EntityWithChanges<T> existingEntity = allEntities.get(dbKey);
            if (existingEntity != null && !existingEntity.getEntity().isDeleted()) {
                T t = existingEntity.getEntity();
                if (existingEntity.isSavedAtHeight(entityCopy.getHeight())) {
                    throw new IllegalStateException("Unable to delete entity at the height of insert. Entity can be deleted at height higher than " + t.getHeight() + " current entity state does not meet that requirement: "+ existingEntity);
                }
                updateExisting(entityCopy, existingEntity);
                existingEntity.becomeDeleted();
                log.info("Deleted entity {} at height {}", entityCopy, entityCopy.getHeight());
                return true;
            } else {
                return false;
            }
        });
    }

    public void trim(int height) {
        inReadLock(() -> {
            Set<DbKey> toRemoveEntirely = new HashSet<>();
            allEntities.forEach((key, l) -> {
                rows -= l.trim(height);
                if (l.isEmpty()) {
                    toRemoveEntirely.add(key);
                }
            });
            toRemoveEntirely.forEach(allEntities::remove);
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
                        DbIdLatestValue lastDbIdLatestValue = last(dbIdLatestValues).orElseThrow(()-> new IllegalStateException("Db rollback inconsistent behavior, entity to update "
                            + e + " should have at least one DbIdLatestValue to be correctly represented in the cache. Error at rollback to height: " + height));
                        entity.setDbId(lastDbIdLatestValue.getDbId());
                        entity.setHeight(lastDbIdLatestValue.getHeight());
                        lastDbIdLatestValue.makeLatest();
                    });
            keysToRenewDeleted.stream().map(allEntities::get).forEach(e-> {
                T entity = e.getEntity();
                entity.setDeleted(false);
                last(e.getDbIdLatestValues()).orElseThrow(() -> new IllegalStateException("Unable to renew deleted entity, " +
                    "required at least one DbIdLatestValue to renew entity " + e + " during rollback at height " + height + ". Possible cause: incorrect insert operation"))
                .makeLatest();
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
                    .map(this::copyEntity)
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
                .sorted(Comparator.comparingLong(DerivedEntity::getDbId)).map(this::copyEntity), from, to));
    }

    protected Stream<T> latestStream() {
        return allEntities.values()
            .stream()
            .map(EntityWithChanges::getEntity)
            .filter(VersionedDerivedEntity::isLatest) // skip previous versions
            .filter(e-> !e.isDeleted()); // skip deleted
    }

    private List<T> reconstructHistoricalEntries(EntityWithChanges<T> entityWithChanges) {
        List<T> historicalEntities = new ArrayList<>();
        entityWithChanges.getDbIdLatestValues().forEach(e -> {
            T historicalEntity = copyEntity(entityWithChanges.getEntity());
            historicalEntity.setDbId(e.getDbId());
            historicalEntity.setHeight(e.getHeight());
            historicalEntity.setLatest(e.isLatest());
            historicalEntity.setDeleted(e.isDeleted());
            changeableColumns.forEach(column -> {
                Optional<Object> columnValueOpt = entityWithChanges.getValueForColumnAtHeight(column, e.getHeight());
                Object columnValue = columnValueOpt.orElse(null);
                setColumn(column, columnValue, historicalEntity);
            });
            historicalEntities.add(historicalEntity);
        });
        return historicalEntities;
    }

    private void updateExisting(T entityCopy, EntityWithChanges<T> existingEntity) {
        if (existingEntity.getEntity().getHeight() == entityCopy.getHeight()) { // do merge
            Map<String, List<Change>> changes = existingEntity.getChanges();
            boolean prevDeleted = existingEntity.getEntity().isDeleted();
            changeableColumns.forEach(c -> {
                List<Change> columnChanges = changes.get(c);
                Optional<Change> lastColumnChangeOpt = last(columnChanges);
                Value value = analyzeChanges(c, prevDeleted ? null : lastColumnChangeOpt.map(Change::getValue).orElse(null), entityCopy);
                if (value.isChanged()) { // change was performed
                    if (!prevDeleted && lastColumnChangeOpt.isPresent() && lastColumnChangeOpt.get().getHeight() == entityCopy.getHeight()) {
                        lastColumnChangeOpt.get().setValue(value.getV());
                    } else {
                        columnChanges.add(new Change(entityCopy.getHeight(), value.getV()));
                    }
                }
            });
            existingEntity.setEntity(entityCopy);
            existingEntity.undoDeleted();
            log.info("Merge in-memory entity {}, total entities {}", entityCopy, rows);
        } else { // do insert new value
            boolean prevDeleted = existingEntity.getEntity().isDeleted();
            Map<String, List<Change>> changes = existingEntity.getChanges();
            changeableColumns.forEach(c -> {
                List<Change> columnChanges = changes.get(c);
                Object prevValue = getLastValueOrNull(columnChanges);
                Value value = analyzeChanges(c, prevDeleted ? null : prevValue, entityCopy);
                if (value.isChanged()) { // new value exists or equal to null
                    columnChanges.add(new Change(entityCopy.getHeight(), value.getV()));
                }
            });
            List<DbIdLatestValue> dbIdLatestValues = existingEntity.getDbIdLatestValues();
            last(dbIdLatestValues).orElseThrow(()-> new IllegalStateException("Expected at least one DbIdLatestValue for the existing entity: "
                + existingEntity + " during insert of the new entity " + entityCopy)
            ).setLatest(false);
            dbIdLatestValues.add(new DbIdLatestValue(entityCopy.getHeight(), true, false, entityCopy.getDbId()));
            existingEntity.setEntity(entityCopy);
            rows++;
            log.info("Updated in-memory entity {}, total entities {}", entityCopy, rows);
        }
    }

    private T copyEntity(T entity) {
        return (T) entity.deepCopy();
    }

    private <V> Optional<V> last(List<V> l) {
        if (l == null) {
            return Optional.empty();
        }
        if (l.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(l.get(l.size() - 1));
    }

    @ToString
    public static class Value {
        private boolean changed;
        private Object v;

        public boolean isChanged() {
            return changed;
        }

        public boolean isNull() {
            return v == null;
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

