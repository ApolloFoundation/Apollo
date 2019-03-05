/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Singleton;

/**
 * This is regitry for tables that is used in BlockchainProcessorImpl for 
 * quite mysterious purposes...
 * @author al
 */

public class DerivedDbTablesRegistry {
     private final List<DerivedDbTable> derivedTables = new CopyOnWriteArrayList<>();
     private static DerivedDbTablesRegistry instance = new DerivedDbTablesRegistry();
     
    public static DerivedDbTablesRegistry getInstance(){
        return instance;
    } 
    public void registerDerivedTable(DerivedDbTable table) {
        derivedTables.add(table);
    } 
    public List<DerivedDbTable> getDerivedTables() {
        return derivedTables;
    }     
  }
