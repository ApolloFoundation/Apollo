/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class EntityWithChanges<T extends VersionedDerivedEntity> {
    // blockchain entity, which hold final data (which cannot be changed)
    private T entity;
    // key is a column name, value -> list of height based values for this column
    private Map<String, List<Change>> changes;
}
