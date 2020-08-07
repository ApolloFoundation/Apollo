/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.converter.db.dgs.DGSGoodsMapper;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Singleton
public class DGSGoodsTable extends EntityDbTable<DGSGoods> implements SearchableTableInterface<DGSGoods> {
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
        super(TABLE_NAME, KEY_FACTORY, true, FULL_TEXT_SEARCH_COLUMNS, false);
    }

    @Override
    public DGSGoods load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        DGSGoods goods = MAPPER.map(rs, null);
        goods.setDbKey(dbKey);
        return goods;
    }

    @Override
    public void save(Connection con, DGSGoods goods) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO goods (id, seller_id, name, "
                + "description, tags, parsed_tags, timestamp, quantity, price, delisted, has_image, height, latest) KEY (id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")
        ) {
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
    public String defaultSort() {
        return " ORDER BY timestamp DESC, id ASC ";
    }

    public DGSGoods get(long purchaseId) {
        return get(KEY_FACTORY.newKey(purchaseId));
    }

    @Override
    public final DbIterator<DGSGoods> search(String query, DbClause dbClause, int from, int to) {
        return search(query, dbClause, from, to, " ORDER BY ft.score DESC ");
    }

    @Override
    public final DbIterator<DGSGoods> search(String query, DbClause dbClause, int from, int to, String sort) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement("SELECT " + table + ".*, ft.score FROM " + table +
                ", ftl_search('PUBLIC', '" + table + "', ?, 2147483647, 0) ft "
                + " WHERE " + table + ".db_id = ft.keys[1] "
                + (multiversion ? " AND " + table + ".latest = TRUE " : " ")
                + " AND " + dbClause.getClause() + sort
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setString(++i, query);
            i = dbClause.set(pstmt, ++i);
            i = DbUtils.setLimits(i, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }


}
