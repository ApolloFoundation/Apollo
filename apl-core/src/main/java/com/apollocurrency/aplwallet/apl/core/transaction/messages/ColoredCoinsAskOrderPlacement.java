/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacementAttachment {

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
    public TransactionTypes.TransactionTypeSpec getTransactionType() {
        return TransactionTypes.TransactionTypeSpec.CC_ASK_ORDER_PLACEMENT;
    }

}
