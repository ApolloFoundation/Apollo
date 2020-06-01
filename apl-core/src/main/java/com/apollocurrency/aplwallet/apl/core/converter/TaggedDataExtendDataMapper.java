/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter;

import com.apollocurrency.aplwallet.apl.core.entity.operation.tagged.TaggedDataExtend;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TaggedDataExtendDataMapper implements RowMapper<TaggedDataExtend> {

    @Override
    public TaggedDataExtend map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TaggedDataExtend(rs, null);
    }
}
