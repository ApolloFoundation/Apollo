/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
@EqualsAndHashCode(callSuper = true)
public class MonetarySystemCurrencyBurningAttachment extends AbstractAttachment implements MonetarySystemAttachment {
    final long currencyId;
    final long units;

    public MonetarySystemCurrencyBurningAttachment(ByteBuffer buffer) {
        super(buffer);
        this.currencyId = buffer.getLong();
        this.units = buffer.getLong();
    }

    public MonetarySystemCurrencyBurningAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.units = Convert.parseLong(attachmentData.get("units"));
    }

    public MonetarySystemCurrencyBurningAttachment(long currencyId, long units) {
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
        return TransactionTypes.TransactionTypeSpec.MS_CURRENCY_BURNING;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

    public long getUnits() {
        return units;
    }
}
