/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class EntityWithChanges<T extends VersionedDeletableEntity> {
    // blockchain entity, which hold final data (which cannot be changed)
    private T entity;
    // key is a column name, value -> list of height based values for this column
    private Map<String, List<Change>> changes;
    // list which store dbId, latest and height properties for entity changes
    private List<DbIdLatestValue> dbIdLatestValues;
    // min height where entity still exists
    private int minHeight;

    public Optional<Object> getValueForColumnAtHeight(String column, int height) {
        if (height < minHeight) {
            throw new IllegalArgumentException("Too low height " + height + " to get changes, no data there for entity: " + this);
        }
        List<Change> changes = this.changes.get(column);
        if (changes == null) {
            throw new IllegalArgumentException("Unknown column " + column + " for the entity " + this);
        }
        Optional<Change> columnOptionalValue = changes.stream().filter(c -> c.getHeight() <= height).max(Comparator.comparingInt(Change::getHeight));
        return columnOptionalValue.flatMap(o -> Optional.ofNullable(o.getValue()));
    }

    public boolean isEmpty() {
        return dbIdLatestValues.isEmpty();
    }

    public int trim(int height) {
        if (minHeight >= height) {
            return 0;
        }
        long trimCandidates = dbIdLatestValues.stream().filter(s -> s.getHeight() < height).count();
        int maxHeight = dbIdLatestValues.stream().filter(s -> s.getHeight() < height).sorted(Comparator.comparing(DbIdLatestValue::getHeight).reversed()).map(DbIdLatestValue::getHeight).findFirst().orElse(0);
        if (trimCandidates <= 1) {
            return 0;
        }
        int trimmed;
        if (isFullyDeletedAtHeight(maxHeight)) {
            trimmed = removeChangesBefore(maxHeight);
        } else {
            moveColumnChangesToHeight(maxHeight);
            int prevRows = dbIdLatestValues.size();
            dbIdLatestValues.removeIf(s -> s.getHeight() < maxHeight);
            trimmed = prevRows - dbIdLatestValues.size();
        }
        minHeight = dbIdLatestValues.stream().sorted(Comparator.comparingInt(DbIdLatestValue::getHeight)).map(DbIdLatestValue::getHeight).findFirst().orElse(-1);
        return trimmed;
    }

    public boolean isSavedAtHeight(int height) {
        return minHeight == height && dbIdLatestValues.size() == 1;
    }

    private void moveColumnChangesToHeight(int height) {
        for (Map.Entry<String, List<Change>> columnWithChanges : changes.entrySet()) {
            List<Change> changes = columnWithChanges.getValue();
            Optional<Change> lastChangeBeforeHeightOpt = changes.stream().filter(c -> c.getHeight() <= height).max(Comparator.comparingInt(Change::getHeight));
            if (lastChangeBeforeHeightOpt.isEmpty()) { // null value if empty
                return;
            }
            Change lastChange = lastChangeBeforeHeightOpt.get();
            if (lastChange.getValue() != null) {
                lastChange.setHeight(height);
            }
            changes.removeIf(c -> c.getHeight() < height);
        }
    }

    private boolean isFullyDeletedAtHeight(int height) {
        List<DbIdLatestValue> allDeletedUpdates = dbIdLatestValues.stream().filter(s -> s.getHeight() <= height && s.isDeleted()).sorted(Comparator.comparing(DbIdLatestValue::getHeight).reversed()).collect(Collectors.toList());
        return !allDeletedUpdates.isEmpty() && allDeletedUpdates.get(0).getHeight() == height && allDeletedUpdates.size() % 2 == 0; // fully finished delete at height less or equal than given
    }

    private int removeChangesBefore(int height) {
        int initialSize = dbIdLatestValues.size();
        if (height >= entity.getHeight()) {
            dbIdLatestValues.clear();
            changes.clear();
        } else {
            dbIdLatestValues.removeIf(e -> e.getHeight() <= height);
            changes.forEach((c, columnChanges)-> columnChanges.removeIf(e -> e.getHeight() <= height));
        }
        return initialSize - dbIdLatestValues.size();
    }

    public void becomeDeleted() {
        if (dbIdLatestValues.size() < 2) {
            throw new IllegalStateException("Unable to become deleted, entity is inconsistent state, no enough dbIdLatest values, required at least 2: " + this);
        }
        entity.setLatest(false);
        entity.setDeleted(false);
        dbIdLatestValues.get(dbIdLatestValues.size() - 1).makeDeleted();
        dbIdLatestValues.get(dbIdLatestValues.size() - 2).makeDeleted();
    }

    public void undoDeleted() {
        if (dbIdLatestValues.isEmpty()) {
            throw new IllegalStateException("Unable to undo deleted, entity is inconsistent state, no dbIdLatest values: " + this);
        }
        entity.setLatest(true);
        entity.setDeleted(false);
        dbIdLatestValues.get(dbIdLatestValues.size() - 1).makeLatest();
        if (dbIdLatestValues.size() >= 2) {
            dbIdLatestValues.get(dbIdLatestValues.size() - 2).makeVersioned();
        }
    }
}
