/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class MultiversionEntityDbTableTest<T extends VersionedDerivedEntity> extends EntityDbTableTest<T> {
    public MultiversionEntityDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey();
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 1 && !e.getValue().get(0).isLatest()), "At least one blockchain deleted record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 1 && e.getValue().get(0).isLatest()), "At least one not updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 2 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest()), "At least one updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 3 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest() && !e.getValue().get(2).isLatest()), "At least one updated twice record should exist");
    }

    @Override
    public List<T> getAllLatest() {
        return sortByHeightDesc(getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList()));
    }

    @Override
    public T getDeletedMultiversionRecord() {
        return groupByDbKey().entrySet().stream().filter(e -> e.getValue().size() == 1 && !e.getValue().get(0).isLatest()).map(e-> e.getValue().get(0)).findAny().get();
    }
}
