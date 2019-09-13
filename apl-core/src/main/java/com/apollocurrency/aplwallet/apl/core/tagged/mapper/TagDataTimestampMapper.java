/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataTimestamp;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class TagDataTimestampMapper implements RowMapper<TaggedDataTimestamp> {

    @Override
    public TaggedDataTimestamp map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TaggedDataTimestamp(rs, null);
    }
}
