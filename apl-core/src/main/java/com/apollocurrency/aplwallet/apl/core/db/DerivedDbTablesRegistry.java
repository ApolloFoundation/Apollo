/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Singleton;

/**
 * This is registry for tables that is used in TrimService for deleting old data from derived tables
 *
 * @author al
 */
@Singleton
public class DerivedDbTablesRegistry {
    private final List<DerivedDbTable> derivedTables = new CopyOnWriteArrayList<>();
    public void registerDerivedTable(DerivedDbTable table) {
        derivedTables.add(table);
    } 
    public List<DerivedDbTable> getDerivedTables() {
        return derivedTables;
    }     
  }
