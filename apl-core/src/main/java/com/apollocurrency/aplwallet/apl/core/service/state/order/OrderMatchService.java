/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.order;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
public interface OrderMatchService {
    void addAskOrder(Transaction transaction, ColoredCoinsAskOrderPlacement attachment);

    void addBidOrder(Transaction transaction, ColoredCoinsBidOrderPlacement attachment);
}