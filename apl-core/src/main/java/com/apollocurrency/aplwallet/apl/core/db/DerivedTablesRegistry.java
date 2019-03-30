/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import java.util.Collection;

public interface DerivedTablesRegistry {
    void registerDerivedTable(DerivedDbTable table);

    Collection<DerivedDbTable> getDerivedTables();
}
