/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.mapper;

import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DGSGoodsMapper extends VersionedDerivedEntityMapper<DGSGoods> {

    public DGSGoodsMapper(KeyFactory<DGSGoods> keyFactory) {
        super(keyFactory);
    }

    @Override
    public DGSGoods doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long sellerId = rs.getLong("seller_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String tags = rs.getString("tags");
        String[] parsedTags = DbUtils.getArray(rs, "parsed_tags", String[].class);
        int quantity = rs.getInt("quantity");
        long priceATM = rs.getLong("price");
        boolean delisted = rs.getBoolean("delisted");
        int timestamp = rs.getInt("timestamp");
        boolean hasImage = rs.getBoolean("has_image");
        return new DGSGoods(null, null, id, sellerId, name, description, tags, parsedTags, timestamp, hasImage, quantity, priceATM, delisted);
    }
}
