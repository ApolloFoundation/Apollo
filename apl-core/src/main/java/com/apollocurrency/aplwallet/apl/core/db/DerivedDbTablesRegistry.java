/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;

/**
 * This is registry for tables that is used in BlockchainProcessorImpl for
 * quite mysterious purposes...
 * @author al
 */

public class DerivedDbTablesRegistry {
     private final List<DerivedTableInterface> derivedTables = new CopyOnWriteArrayList<>();
     private static DerivedDbTablesRegistry instance = new DerivedDbTablesRegistry();
     
    public static DerivedDbTablesRegistry getInstance(){
        return instance;
    }

    public void registerDerivedTable(DerivedTableInterface table) {
        derivedTables.add(table);
    }

    public List<DerivedTableInterface> getDerivedTables() {
        return derivedTables;
    }     
  }
