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
public final class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {
    
    public ColoredCoinsAskOrderPlacement(ByteBuffer buffer) {
        super(buffer);
    }

    public ColoredCoinsAskOrderPlacement(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ColoredCoinsAskOrderPlacement(long assetId, long quantityATU, long priceATM) {
        super(assetId, quantityATU, priceATM);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoins.ASK_ORDER_PLACEMENT;
    }
    
}
