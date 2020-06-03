/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.tagged;

import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TaggedDataMapper implements RowMapper<TaggedData> {

    @Override
    public TaggedData map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TaggedData(rs, null);
    }
}
