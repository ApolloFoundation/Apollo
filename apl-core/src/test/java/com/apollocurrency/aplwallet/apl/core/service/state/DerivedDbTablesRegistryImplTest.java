/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class DerivedDbTablesRegistryImplTest {

    DerivedDbTablesRegistryImpl registry = new DerivedDbTablesRegistryImpl();


    @Test
    void registerDerivedTable() {
        DerivedTableInterface<?> accountTable = mockTable("account");
        DerivedTableInterface<?> shufflingTable = mockTable("shuffling");

        registry.registerDerivedTable(accountTable);
        registry.registerDerivedTable(shufflingTable);

        verifyRegisteredTable(accountTable);
        verifyRegisteredTable(shufflingTable);
    }

    private void verifyRegisteredTable(DerivedTableInterface<?> table) {
        DerivedTableInterface<? extends DerivedEntity> retrievedAccountTable = registry.getDerivedTable(table.getName());

        assertSame(table, retrievedAccountTable);


        Collection<String> derivedTableNames = registry.getDerivedTableNames();

        assertTrue(derivedTableNames.contains(table.getName()), "Registry should contain '" + table.getName() + "' table name (since it was registered earlier)");


        Collection<DerivedTableInterface<? extends DerivedEntity>> derivedTables = registry.getDerivedTables();

        assertTrue(derivedTables.contains(table), "Registry should contain '" + table.getName() + "' table (since it was registered earlier)");


        String registryString = registry.toString();

        assertTrue(registryString.contains(table.getName()), "Registry toString representation should contain '" + table.getName() + "' table name");
    }

    private DerivedTableInterface<?> mockTable(String name) {
        DerivedTableInterface<?> table = mock(DerivedTableInterface.class);
        doReturn(name).when(table).getName();
        return table;
    }
}