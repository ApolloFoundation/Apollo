/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.types.ms.MonetarySystemTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class MonetarySystemExchangeBuyAttachment extends MonetarySystemExchangeAttachment {

    public MonetarySystemExchangeBuyAttachment(ByteBuffer buffer) {
        super(buffer);
    }

    public MonetarySystemExchangeBuyAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public MonetarySystemExchangeBuyAttachment(long currencyId, long rateATM, long units) {
        super(currencyId, rateATM, units);
    }

    @Override
    public TransactionType getTransactionType() {
        return MonetarySystemTransactionType.EXCHANGE_BUY;
    }

}
