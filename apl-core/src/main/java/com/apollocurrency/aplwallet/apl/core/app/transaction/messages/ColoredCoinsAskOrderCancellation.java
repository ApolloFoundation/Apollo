/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.transaction.ColoredCoins;
import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {
    
    public ColoredCoinsAskOrderCancellation(ByteBuffer buffer) {
        super(buffer);
    }

    public ColoredCoinsAskOrderCancellation(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ColoredCoinsAskOrderCancellation(long orderId) {
        super(orderId);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoins.ASK_ORDER_CANCELLATION;
    }
    
}
