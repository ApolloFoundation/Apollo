/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.mapper;

import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSTag;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DGSTagMapper implements RowMapper<DGSTag> {
    @Override
    public DGSTag map(ResultSet rs, StatementContext ctx) throws SQLException {
        String tag = rs.getString("tag");
        int inStockCount = rs.getInt("in_stock_count");
        int totalCount = rs.getInt("total_count");
        int height = rs.getInt("height");
        return new DGSTag(tag, inStockCount, totalCount, height);
    }
}
