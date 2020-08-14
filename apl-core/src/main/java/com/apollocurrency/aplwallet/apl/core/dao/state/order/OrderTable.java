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

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.Order;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
@Slf4j
public abstract class OrderTable<T extends Order> extends VersionedDeletableEntityDbTable<T> {

    OrderTable(String tableName, LongKeyFactory<T> longKeyFactory,
               DerivedTablesRegistry derivedDbTablesRegistry,
               DatabaseManager databaseManager) {
        super(tableName, longKeyFactory, null, derivedDbTablesRegistry, databaseManager, null);
    }

    @Override
    public void save(Connection con, T order) throws SQLException {
        log.trace("save table={}, entity={}, stack = {}",
            super.getTableName(), order, ThreadUtils.last5Stacktrace());
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + table + " (id, account_id, asset_id, "
                + "price, quantity, creation_height, transaction_index, transaction_height, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE "
                + "id = VALUES(id), account_id = VALUES(account_id), asset_id = VALUES(asset_id), "
                + "price = VALUES(price), quantity = VALUES(quantity), creation_height = VALUES(creation_height), "
                + "transaction_index = VALUES(transaction_index), transaction_height = VALUES(transaction_height), "
                + "height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, order.getId());
            pstmt.setLong(++i, order.getAccountId());
            pstmt.setLong(++i, order.getAssetId());
            pstmt.setLong(++i, order.getPriceATM());
            pstmt.setLong(++i, order.getQuantityATU());
            pstmt.setInt(++i, order.getCreationHeight());
            pstmt.setShort(++i, order.getTransactionIndex());
            pstmt.setInt(++i, order.getTransactionHeight());
            pstmt.setInt(++i, order.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public String defaultSort() {
        return " ORDER BY creation_height DESC ";
    }
}
