package com.apollocurrency.aplwallet.apl.core.db;

import java.util.Map;

public interface TableCache {

    Map<DbKey,Object> getCache(String tableName);

    void clearCache(String tableName);

    void clearCache();
}
