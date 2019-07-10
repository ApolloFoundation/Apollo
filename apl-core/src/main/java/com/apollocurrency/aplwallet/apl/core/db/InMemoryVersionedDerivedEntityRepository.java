/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This implementation provide additional features (implemented trim and delete) and provide
 * support for versioned entities (with latest column)
 * @param <T> versioned derived entity to store
 */
public class InMemoryVersionedDerivedEntityRepository<T extends VersionedDerivedEntity> extends InMemoryDerivedEntityRepository<T> {
    @Override
    public void insert(T entity) {
        entity.setLatest(true);
        super.insert(entity);
    }

    @Override
    protected void doInsert(List<T> allEntities, T newEntity, T prevEntity) {
        prevEntity.setLatest(false);
        super.doInsert(allEntities, newEntity, prevEntity);
    }

    @Override
    public void delete(T entity) {
        List<T> existingEntities = getAllEntities().get(entity.getDbKey());
        entity.setLatest(false);
        if (existingEntities != null) {
            int lastPosition = existingEntities.size() - 1; // assume that existing list of entities has min size 1
            T existingEntity = existingEntities.get(lastPosition);
            existingEntity.setLatest(false);
            existingEntities.add(entity);
        }
    }

    @Override
    public void trim(int height) {
        getAllEntities().forEach((id, l)-> {
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
        });
    }


    @Override
    public void rollback(int height) {
       super.rollback(height);
       getAllEntities().values().forEach(l-> l.get(l.size() - 1).setLatest(true));
    }
}
