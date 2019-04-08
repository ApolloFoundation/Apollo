/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import java.util.Set;

public interface FullTextConfig {
    void registerTable(String tableName);

    void setSchema(String schema);

    Set<String> getTableNames();

    String getSchema();
}
