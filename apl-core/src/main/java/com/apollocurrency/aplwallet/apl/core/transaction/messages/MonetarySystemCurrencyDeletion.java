/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MonetarySystemCurrencyDeletion extends AbstractAttachment implements MonetarySystemAttachment {
    
    final long currencyId;

    public MonetarySystemCurrencyDeletion(ByteBuffer buffer) {
        super(buffer);
        this.currencyId = buffer.getLong();
    }

    public MonetarySystemCurrencyDeletion(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
    }

    public MonetarySystemCurrencyDeletion(long currencyId) {
        this.currencyId = currencyId;
    }

    @Override
    int getMySize() {
        return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("currency", Long.toUnsignedString(currencyId));
    }

    @Override
    public TransactionType getTransactionType() {
        return MonetarySystem.CURRENCY_DELETION;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }
    
}
