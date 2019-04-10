/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import java.nio.file.Path;
import java.util.Set;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class FullTextConfigProducer {
    @Inject
    FullTextConfig fullTextConfig;
    
    @Produces
    @Named("fullTextTables")
    Set<String> produceFullTextTables() {
        return fullTextConfig.getTableNames();
    }

    @Produces
    @Named("tablesSchema")
    String produceTablesSchema() {
        return fullTextConfig.getSchema();

    }

    @Produces
    @Named("indexDirPath")
    public Path produceIndexPath() {
        return RuntimeEnvironment.getInstance().getDirProvider().getFullTextSearchIndexDir();
    }
}
