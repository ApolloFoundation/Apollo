/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import java.nio.file.Path;
import java.util.List;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class FullTextConfigProducer {
    @Produces
    @Named("fullTextTables")
    List<String> produceFullTextTables() {
        return FullTextConfig.getInstance().getTableNames();
    }
    @Produces
    @Named("tablesSchema")
    String produceTablesSchema() {
        return FullTextConfig.getInstance().getSchema();
    }

    @Produces
    @Named("indexDirPath")
    Path produceIndexPath() {
        return RuntimeEnvironment.getInstance().getDirProvider().getFullTextSearchIndexDir();
    }
}
