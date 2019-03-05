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
public final class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacementAttachment {
    
    public ColoredCoinsBidOrderPlacement(ByteBuffer buffer) {
        super(buffer);
    }

    public ColoredCoinsBidOrderPlacement(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ColoredCoinsBidOrderPlacement(long assetId, long quantityATU, long priceATM) {
        super(assetId, quantityATU, priceATM);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoins.BID_ORDER_PLACEMENT;
    }
    
}
