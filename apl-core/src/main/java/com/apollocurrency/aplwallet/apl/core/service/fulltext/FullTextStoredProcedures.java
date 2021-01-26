/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;

import javax.enterprise.inject.spi.CDI;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provide stored procedures, used for fulltext search.
 * Should be removed as soon as possible, but now this class should exists due to {@link EntityDbTable#search(String, DbClause, int, int, String)}
 * Deep refactoring of EntityDbTable and dependent classes required to resolve issue
 *
 * @deprecated
 */
@Deprecated
public class FullTextStoredProcedures {
    public static ResultSet search(String schema, String table, String queryText, int limit, int offset)
        throws SQLException {
        return CDI.current().select(FullTextSearchService.class).get().search(schema, table, queryText, limit, offset);
    }
}
