/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static java.util.stream.Collectors.groupingBy;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In memory repository for blockchain entities,
 * Main purpose: to store derived entities and maintain data consistency according to blockchain events
 * Supports rollback, trim, insert operations
 * @param <T> derived entity to store
 */
public class InMemoryDerivedEntityRepository<T extends DerivedEntity> {
    private Map<DbKey, List<T>> allEntities = new ConcurrentHashMap<>();

    protected Map<DbKey, List<T>> getAllEntities() {
        return allEntities;
    }

    public void putAll(List<T> objects) {
        allEntities.putAll(objects.stream()
                .collect(groupingBy(DerivedEntity::getDbKey,
                        Collectors.collectingAndThen(Collectors.toList(), l -> l.stream()
                                .sorted(Comparator.comparing(DerivedEntity::getHeight))
                                .collect(Collectors.toList())))));
    }

    public void clear() {
        allEntities.clear();
    }

    public void rollback(int height) {
        allEntities.values().forEach(l-> l.removeIf(s->s.getHeight() > height));
        allEntities.values().forEach(l-> l.removeIf(s->s.getHeight() > height));
        List<DbKey> dbKeysToDelete = allEntities.entrySet().stream().filter(e -> e.getValue().size() == 0).map(Map.Entry::getKey).collect(Collectors.toList());
        dbKeysToDelete.forEach(dbKey -> allEntities.remove(dbKey));
    }

    public T get(DbKey dbKey) {
        List<T> entities = allEntities.get(dbKey);

        return entities == null ? null : entities.get(entities.size() - 1);
    }

    public T getCopy(DbKey dbKey) {
        List<T> existingEntities = allEntities.get(dbKey);
        if (existingEntities != null) {
            T lastObject = existingEntities.get(existingEntities.size() - 1);
            try {
                return (T) lastObject.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.toString(), e);
            }
        } else {
            return null;
        }
    }

    public void insert(T entity) {
        List<T> existingEntities = allEntities.get(entity.getDbKey());
        if (existingEntities == null) {
            List<T> entities = new ArrayList<>();
            entities.add(entity);
            allEntities.put(entity.getDbKey(), entities);
        } else {
            int lastPosition = existingEntities.size() - 1; // assume that existing list of entities has min size 1
            T lastEntity = existingEntities.get(lastPosition);
            if (lastEntity.getHeight() == entity.getHeight()) {
                existingEntities.set(lastPosition, entity); // do merge
            } else {
                doInsert(existingEntities, entity, lastEntity); // do insert new value
            }
        }
    }

    protected void doInsert(List<T> allEntities, T newEntity, T prevEntity) {
        allEntities.add(newEntity);
    }

    public void delete(T entity) {

    }

    public void trim(int height) {

    }

}
