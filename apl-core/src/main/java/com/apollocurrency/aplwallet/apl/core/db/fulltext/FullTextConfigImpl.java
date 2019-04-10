/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.util.StringValidator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Singleton;

@Singleton
public class FullTextConfigImpl implements FullTextConfig {

    private Set<String> tableNames = new HashSet<>();
    private String schema = "PUBLIC";

    public synchronized void registerTable(String tableName) {
        StringValidator.requireNonBlank(tableName, "Table name");
        tableNames.add(tableName);
    }

    public void setSchema(String schema) {
        StringValidator.requireNonBlank(schema, "Schema");
        this.schema = schema;
    }

    public FullTextConfigImpl() {
    }

    public synchronized Set<String> getTableNames() {
        return Collections.unmodifiableSet(tableNames);
    }

    public String getSchema() {
        return schema;
    }
}
