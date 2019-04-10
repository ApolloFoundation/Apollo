/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

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
    private final Map<String, DerivedDbTable> derivedTables = new ConcurrentHashMap<>();
    public void registerDerivedTable(DerivedDbTable table) {
        derivedTables.putIfAbsent(table.toString(), table);
    } 
    public Collection<DerivedDbTable> getDerivedTables() {
        return derivedTables.values();
    }

    public DerivedDbTablesRegistryImpl() {
    }
}
