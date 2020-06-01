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

package com.apollocurrency.aplwallet.apl.core.dao.operation.order;

import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.operation.order.Order;
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

    OrderTable(String tableName, LongKeyFactory<T> longKeyFactory) {
        super(tableName, longKeyFactory, false);
    }

    @Override
    public void save(Connection con, T order) throws SQLException {
        log.trace("save table={}, entity={}, stack = {}",
            super.getTableName(), order, ThreadUtils.last5Stacktrace());
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, account_id, asset_id, "
                + "price, quantity, creation_height, transaction_index, transaction_height, height, latest, deleted) KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
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
