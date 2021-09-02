/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.order;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
@Singleton
public class BidOrderTable extends OrderTable<BidOrder> {
    private static final LongKeyFactory<BidOrder> bidOrderDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(BidOrder orderBid) {
            if (orderBid.getDbKey() == null) {
                orderBid.setDbKey(super.newKey(orderBid.getId()));
            }
            return orderBid.getDbKey();
        }
    };

    @Inject
    public BidOrderTable(DerivedTablesRegistry derivedDbTablesRegistry,
                         DatabaseManager databaseManager,
                         Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("bid_order", bidOrderDbKeyFactory, databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public BidOrder load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new BidOrder(rs, dbKey);
    }

    public BidOrder getBidOrder(long orderId) {
        return get(bidOrderDbKeyFactory.newKey(orderId));
    }

    public BidOrder getNextOrder(DataSource dataSource, long assetId) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM bid_order WHERE asset_id = ? "
                 + "AND latest = TRUE ORDER BY price DESC, creation_height ASC, transaction_height ASC, transaction_index ASC LIMIT 1")) {
            pstmt.setLong(1, assetId);
            try (DbIterator<BidOrder> bidOrders = getManyBy(con, pstmt, true)) {
                return bidOrders.hasNext() ? bidOrders.next() : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
