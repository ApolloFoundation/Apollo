/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

/**
 * Provide stored procedures, used for fulltext search.
 * Should be removed as soon as possible, but now this class should exists due to {@link com.apollocurrency.aplwallet.apl.core.db.EntityDbTable#search(String, DbClause, int, int, String)}
 * Deep refactoring of EntityDbTable and dependent classes required to resolve issue
 *
 */
public class FullTextStoredProcedures {
    private static FullTextSearchServiceImpl fullTextSearchProvider = CDI.current().select(FullTextSearchServiceImpl.class).get();
    public static ResultSet search(String schema, String table, String queryText, int limit, int offset)
            throws SQLException {
        return fullTextSearchProvider.search(schema, table, queryText, limit, offset);
    }
}
