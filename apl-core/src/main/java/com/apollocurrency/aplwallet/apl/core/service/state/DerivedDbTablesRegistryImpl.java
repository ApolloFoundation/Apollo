/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This is registry for tables that is used in TrimService for deleting old data from derived tables, in the ShardImporter to import
 * downloaded shard, withing the shard process to export all the blockchain-related data, in the BlockchainProcessor
 * to perform scan and rollback of the blockchain
 * @author al
 */
@Singleton
public class DerivedDbTablesRegistryImpl implements DerivedTablesRegistry {
    private final Map<String, DerivedTableInterface<? extends DerivedEntity>> derivedTables = new ConcurrentHashMap<>();

    @Override
    public void registerDerivedTable(DerivedTableInterface<?> table) {
        derivedTables.putIfAbsent(table.getName(), table);
    }

    @Override
    public Collection<String> getDerivedTableNames() {
        return derivedTables.keySet();
    }

    @Override
    public Collection<DerivedTableInterface<? extends DerivedEntity>> getDerivedTables() {
        return derivedTables.values();
    }

    @Override
    public DerivedTableInterface<? extends DerivedEntity> getDerivedTable(String derivedTableName) {
        return derivedTables.get(derivedTableName);
    }

    @Override
    public String toString() {
        return "DerivedDbTablesRegistry { size:" + getDerivedTables().size() +
            ", tables:[" + getDerivedTableNames().stream().sorted().collect(Collectors.joining(",")) +
            "] }";
    }
}
