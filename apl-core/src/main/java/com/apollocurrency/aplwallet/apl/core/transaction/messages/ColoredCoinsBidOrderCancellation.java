/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.ColoredCoins;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellationAttachment {
    
    public ColoredCoinsBidOrderCancellation(ByteBuffer buffer) {
        super(buffer);
    }

    public ColoredCoinsBidOrderCancellation(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ColoredCoinsBidOrderCancellation(long orderId) {
        super(orderId);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoins.BID_ORDER_CANCELLATION;
    }
    
}
