/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;

import java.nio.file.Path;
import java.util.List;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

public class FullTextConfigProducer {
    @Inject
    FullTextConfig fullTextConfig;
    @Produces
    @Named("fullTextTables")
    List<String> produceFullTextTables() {
        return fullTextConfig.getTableNames();
    }
    @Produces
    @Named("tablesSchema")
    String produceTablesSchema() {
        return fullTextConfig.getSchema();
    }

    @Produces
    @Named("indexDirPath")
    Path produceIndexPath() {
        return AplCoreRuntime.getInstance().getDirProvider().getFullTextSearchIndexDir();
    }
}
