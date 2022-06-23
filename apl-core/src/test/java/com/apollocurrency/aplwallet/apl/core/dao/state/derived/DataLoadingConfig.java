/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andrii Boiarskyi
 * @see
 * @since 1.0.0
 */
public class DataLoadingConfig {
    private static class DataLoadingConfigHolder {
        private static final DataLoadingConfig INSTANCE = new DataLoadingConfig();
    }

    public static DataLoadingConfig getInstance() {
        return DataLoadingConfigHolder.INSTANCE;
    }

    private final Map<String, String> tableDataFileMap = new ConcurrentHashMap<>();

    public DataLoadingConfig() {
        tableDataFileMap.put("phasing_poll", "db/phasing-poll-data.sql");
    }

    public String getDataFile(String table) {
        return tableDataFileMap.getOrDefault(table, "db/data.sql");
    }
}
