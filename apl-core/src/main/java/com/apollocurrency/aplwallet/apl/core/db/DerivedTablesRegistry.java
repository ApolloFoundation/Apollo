/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;

import java.util.Collection;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;

public interface DerivedTablesRegistry {
    void registerDerivedTable(DerivedTableInterface table);

    Collection<DerivedTableInterface> getDerivedTables();
}
