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
