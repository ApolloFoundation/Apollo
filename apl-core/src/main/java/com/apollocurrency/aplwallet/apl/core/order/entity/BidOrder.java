/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.order.entity;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
@ToString(callSuper = true)
public class BidOrder extends Order {

    public BidOrder(Transaction transaction, ColoredCoinsBidOrderPlacement attachment, int height) {
        super(transaction, attachment, height);
    }

    public BidOrder(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs, dbKey);
    }
}
