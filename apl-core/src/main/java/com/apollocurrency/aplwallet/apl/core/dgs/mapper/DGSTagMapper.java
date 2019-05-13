/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.mapper;

import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSTag;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DGSTagMapper extends DerivedEntityMapper<DGSTag> {


    public DGSTagMapper(KeyFactory<DGSTag> keyFactory) {
        super(keyFactory);
    }

    @Override
    public DGSTag doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        String tag = rs.getString("tag");
        int inStockCount = rs.getInt("in_stock_count");
        int totalCount = rs.getInt("total_count");
        return new DGSTag(null, null, tag, inStockCount, totalCount);
    }
}
