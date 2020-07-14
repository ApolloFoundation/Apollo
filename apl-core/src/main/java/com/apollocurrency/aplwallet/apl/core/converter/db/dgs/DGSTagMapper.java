/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.dgs;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSTag;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DGSTagMapper extends VersionedDerivedEntityMapper<DGSTag> {


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
