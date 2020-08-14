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
public final class MonetarySystemReserveClaim extends AbstractAttachment implements MonetarySystemAttachment {

    final long currencyId;
    final long units;

    public MonetarySystemReserveClaim(ByteBuffer buffer) {
        super(buffer);
        this.currencyId = buffer.getLong();
        this.units = buffer.getLong();
    }

    public MonetarySystemReserveClaim(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.units = Convert.parseLong(attachmentData.get("units"));
    }

    public MonetarySystemReserveClaim(long currencyId, long units) {
        this.currencyId = currencyId;
        this.units = units;
    }

    @Override
    public int getMySize() {
        return 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
        buffer.putLong(units);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("currency", Long.toUnsignedString(currencyId));
        attachment.put("units", units);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_RESERVE_CLAIM;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

    public long getUnits() {
        return units;
    }

}
