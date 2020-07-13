/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FullTextConfigImpl implements FullTextConfig {

    private Set<String> tableNames = ConcurrentHashMap.newKeySet();
    private String schema = "PUBLIC";
    private Path ftlIndexPath;

    public FullTextConfigImpl() {
    }

    public void setFtlIndexPath(Path ftlIndexPath) {
        this.ftlIndexPath = ftlIndexPath;
    }

    public synchronized void registerTable(String tableName) {
        StringValidator.requireNonBlank(tableName, "Table name");
        tableNames.add(tableName);
    }

    @Produces
    @Named("fullTextTables")
    public synchronized Set<String> getTableNames() {
        return Collections.unmodifiableSet(tableNames);
    }

    @Produces
    @Named("tablesSchema")
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        StringValidator.requireNonBlank(schema, "Schema");
        this.schema = schema;
    }

    @Produces
    @Named("indexDirPath")
    public Path getIndexPath() {
        return ftlIndexPath == null ? RuntimeEnvironment.getInstance().getDirProvider().getFullTextSearchIndexDir() : ftlIndexPath;
    }
}
