/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
public final class CCBidOrderPlacementAttachment extends CCOrderPlacementAttachment {

    public CCBidOrderPlacementAttachment(ByteBuffer buffer) {
        super(buffer);
    }

    public CCBidOrderPlacementAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public CCBidOrderPlacementAttachment(long assetId, long quantityATU, long priceATM) {
        super(assetId, quantityATU, priceATM);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_BID_ORDER_PLACEMENT;
    }

}
