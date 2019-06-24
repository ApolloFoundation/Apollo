/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DataTagMapper implements RowMapper<DataTag> {

    @Override
    public DataTag map(ResultSet rs, StatementContext ctx) throws SQLException {
        String tag = rs.getString("tag");
        int height = rs.getInt("height");
        int count = rs.getInt("tag_count");
        return new DataTag(tag, height, count);
    }
}
