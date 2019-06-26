/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DataTagMapper extends VersionedDerivedEntityMapper<DataTag> {

    public DataTagMapper(KeyFactory<DataTag> keyFactory) {
        super(keyFactory);
    }

    @Override
    public DataTag doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        String tag = rs.getString("tag");
        int height = rs.getInt("height");
        int count = rs.getInt("tag_count");
        return new DataTag(tag, height, count);
    }
}
