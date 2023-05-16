/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
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
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("currency", Long.toUnsignedString(currencyId));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_CURRENCY_DELETION;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

}
