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
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

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
public class AskOrderTable extends OrderTable<AskOrder> {
    private static final LongKeyFactory<AskOrder> askOrderDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(AskOrder orderAsk) {
            if (orderAsk.getDbKey() == null) {
                orderAsk.setDbKey(super.newKey(orderAsk.getId()));
            }
            return orderAsk.getDbKey();
        }
    };

    @Inject
    public AskOrderTable(DerivedTablesRegistry derivedDbTablesRegistry,
                         DatabaseManager databaseManager,
                         Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("ask_order", askOrderDbKeyFactory, derivedDbTablesRegistry, databaseManager, deleteOnTrimDataEvent);
    }

    @Override
    public AskOrder load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AskOrder(rs, dbKey);
    }

    public AskOrder getAskOrder(long orderId) {
        return get(askOrderDbKeyFactory.newKey(orderId));
    }

    public AskOrder getNextOrder(DataSource dataSource, long assetId) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ask_order WHERE asset_id = ? "
                 + "AND latest = TRUE ORDER BY price ASC, creation_height ASC, transaction_height ASC, transaction_index ASC LIMIT 1")) {
            pstmt.setLong(1, assetId);
            try (DbIterator<AskOrder> askOrders = getManyBy(con, pstmt, true)) {
                return askOrders.hasNext() ? askOrders.next() : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
