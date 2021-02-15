/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import java.util.Map;

public interface FullTextConfig {
    static String DEFAULT_SCHEMA = "public";

    void registerTable(String tableName, String indexedColumns);

    Map<String, String> getTableNames();

    String getSchema();

    void setSchema(String schema);
}
