/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.converter.db.dgs.DGSGoodsMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    @Inject
    public DGSGoodsTable(DatabaseManager databaseManager,
                         Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, true, FULL_TEXT_SEARCH_COLUMNS,
                databaseManager, deleteOnTrimDataEvent);
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
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO goods (id, seller_id, `name`, "
                    + "description, tags, parsed_tags, `timestamp`, quantity, price, delisted, has_image, height, latest) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) "
                    + "ON DUPLICATE KEY UPDATE id = VALUES(id), seller_id = VALUES(seller_id), `name` = VALUES(`name`), "
                    + "description = VALUES(description), tags = VALUES(tags), parsed_tags = VALUES(parsed_tags), "
                    + "`timestamp` = VALUES(`timestamp`), quantity = VALUES(quantity), price = VALUES(price), "
                    + "delisted = VALUES(delisted), has_image = VALUES(has_image), height = VALUES(height), latest = TRUE",
                Statement.RETURN_GENERATED_KEYS)
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
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    goods.setDbId(rs.getLong(1));
                }
            }
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
        throw new UnsupportedOperationException("Call service, should be implemented by service");
    }

    @Override
    public final DbIterator<DGSGoods> search(String query, DbClause dbClause, int from, int to, String sort) {
        throw new UnsupportedOperationException("Call service, should be implemented by service");
    }


}
