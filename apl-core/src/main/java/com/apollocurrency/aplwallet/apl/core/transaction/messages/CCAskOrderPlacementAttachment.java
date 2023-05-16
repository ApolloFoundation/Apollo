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
public final class CCAskOrderPlacementAttachment extends CCOrderPlacementAttachment {

    public CCAskOrderPlacementAttachment(ByteBuffer buffer) {
        super(buffer);
    }

    public CCAskOrderPlacementAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public CCAskOrderPlacementAttachment(long assetId, long quantityATU, long priceATM) {
        super(assetId, quantityATU, priceATM);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASK_ORDER_PLACEMENT;
    }

}
