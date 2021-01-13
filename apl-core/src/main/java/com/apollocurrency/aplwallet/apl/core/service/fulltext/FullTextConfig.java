/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import java.util.Set;

public interface FullTextConfig {
    void registerTable(String tableName);

    Set<String> getTableNames();

    String getSchema();

    void setSchema(String schema);
}
