/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;

import java.util.Collection;

public interface DerivedTablesRegistry {
    void registerDerivedTable(DerivedTableInterface table);

    Collection<String> getDerivedTableNames();

    Collection<DerivedTableInterface> getDerivedTables();

    DerivedTableInterface getDerivedTable(String derivedTableName);
}
