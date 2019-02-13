/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MonetarySystemExchangeBuy extends MonetarySystemExchange {
    
    public MonetarySystemExchangeBuy(ByteBuffer buffer) {
        super(buffer);
    }

    public MonetarySystemExchangeBuy(JSONObject attachmentData) {
        super(attachmentData);
    }

    public MonetarySystemExchangeBuy(long currencyId, long rateATM, long units) {
        super(currencyId, rateATM, units);
    }

    @Override
    public TransactionType getTransactionType() {
        return MonetarySystem.EXCHANGE_BUY;
    }
    
}
