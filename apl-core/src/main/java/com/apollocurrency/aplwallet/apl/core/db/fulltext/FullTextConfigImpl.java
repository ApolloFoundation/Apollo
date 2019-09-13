/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class FullTextConfigImpl implements FullTextConfig {

    private Set<String> tableNames = new HashSet<>();
    private String schema = "PUBLIC";
    private Path ftlIndexPath;

    public void setFtlIndexPath(Path ftlIndexPath) {
        this.ftlIndexPath = ftlIndexPath;
    }

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

    @Produces
    @Named("indexDirPath")
    public Path getIndexPath() {
        return ftlIndexPath == null ? RuntimeEnvironment.getInstance().getDirProvider().getFullTextSearchIndexDir() : ftlIndexPath;
    }
}
