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
public final class MonetarySystemReserveIncrease extends AbstractAttachment implements MonetarySystemAttachment {
    
    final long currencyId;
    final long amountPerUnitATM;

    public MonetarySystemReserveIncrease(ByteBuffer buffer) {
        super(buffer);
        this.currencyId = buffer.getLong();
        this.amountPerUnitATM = buffer.getLong();
    }

    public MonetarySystemReserveIncrease(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.amountPerUnitATM = attachmentData.containsKey("amountPerUnitATM") ? Convert.parseLong(attachmentData.get("amountPerUnitATM")) : Convert.parseLong(attachmentData.get("amountPerUnitNQT"));
    }

    public MonetarySystemReserveIncrease(long currencyId, long amountPerUnitATM) {
        this.currencyId = currencyId;
        this.amountPerUnitATM = amountPerUnitATM;
    }

    @Override
    int getMySize() {
        return 8 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
        buffer.putLong(amountPerUnitATM);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("currency", Long.toUnsignedString(currencyId));
        attachment.put("amountPerUnitATM", amountPerUnitATM);
    }

    @Override
    public TransactionType getTransactionType() {
        return MonetarySystem.RESERVE_INCREASE;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

    public long getAmountPerUnitATM() {
        return amountPerUnitATM;
    }
    
}
