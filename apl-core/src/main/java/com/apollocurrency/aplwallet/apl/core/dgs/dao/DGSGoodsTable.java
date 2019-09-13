/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.mapper.DGSGoodsMapper;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class DGSGoodsTable extends VersionedDeletableEntityDbTable<DGSGoods> {
    private static final LongKeyFactory<DGSGoods> KEY_FACTORY = new LongKeyFactory<>("id") {

        @Override
        public DbKey newKey(DGSGoods goods) {
            if (goods.getDbKey() == null) {
                goods.setDbKey(KEY_FACTORY.newKey(goods.getId()));
            }
            return goods.getDbKey();
        }
    };
    private static final DGSGoodsMapper MAPPER = new DGSGoodsMapper(KEY_FACTORY);

    private static final String TABLE_NAME = "goods";
    private static final String FULL_TEXT_SEARCH_COLUMNS = "name,description,tags";


    public DGSGoodsTable() {
        super(TABLE_NAME, KEY_FACTORY, FULL_TEXT_SEARCH_COLUMNS, false);
    }

    @Override
    public DGSGoods load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        DGSGoods goods = MAPPER.map(rs, null);
        goods.setDbKey(dbKey);
        return goods;
    }

    @Override
    public void save(Connection con, DGSGoods goods) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO goods (id, seller_id, name, "
                + "description, tags, parsed_tags, timestamp, quantity, price, delisted, has_image, height, latest) KEY (id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, goods.getId());
            pstmt.setLong(++i, goods.getSellerId());
            pstmt.setString(++i, goods.getName());
            pstmt.setString(++i, goods.getDescription());
            pstmt.setString(++i, goods.getTags());
            DbUtils.setArray(pstmt, ++i, goods.getParsedTags());
            pstmt.setInt(++i, goods.getTimestamp());
            pstmt.setInt(++i, goods.getQuantity());
            pstmt.setLong(++i, goods.getPriceATM());
            pstmt.setBoolean(++i, goods.isDelisted());
            pstmt.setBoolean(++i, goods.hasImage());
            pstmt.setInt(++i, goods.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    protected String defaultSort() {
        return " ORDER BY timestamp DESC, id ASC ";
    }

    public DGSGoods get(long purchaseId) {
        return get(KEY_FACTORY.newKey(purchaseId));
    }
}

