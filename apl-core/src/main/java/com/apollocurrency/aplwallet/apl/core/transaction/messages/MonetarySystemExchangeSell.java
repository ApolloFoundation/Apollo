/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MonetarySystemExchangeSell extends MonetarySystemExchangeAttachment {
    
    public MonetarySystemExchangeSell(ByteBuffer buffer) {
        super(buffer);
    }

    public MonetarySystemExchangeSell(JSONObject attachmentData) {
        super(attachmentData);
    }

    public MonetarySystemExchangeSell(long currencyId, long rateATM, long units) {
        super(currencyId, rateATM, units);
    }

    @Override
    public TransactionType getTransactionType() {
        return MonetarySystem.EXCHANGE_SELL;
    }
    
}
