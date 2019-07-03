/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

/**
 * This is registry for tables that is used in TrimService for deleting old data from derived tables
 *
 * @author al
 */
@Singleton
public class DerivedDbTablesRegistryImpl implements DerivedTablesRegistry {
    private final Map<String, DerivedTableInterface> derivedTables = new ConcurrentHashMap<>();

    public void registerDerivedTable(DerivedTableInterface table) {
        derivedTables.putIfAbsent(table.toString(), table);
    }

    @Override
    public Collection<String> getDerivedTableNames() {
        return derivedTables.keySet();
    }

    public Collection<DerivedTableInterface> getDerivedTables() {
        return derivedTables.values();
    }

    @Override
    public DerivedTableInterface getDerivedTable(String derivedTableName) {
        return derivedTables.get(derivedTableName);
    }

    public DerivedDbTablesRegistryImpl() {
    }
}
