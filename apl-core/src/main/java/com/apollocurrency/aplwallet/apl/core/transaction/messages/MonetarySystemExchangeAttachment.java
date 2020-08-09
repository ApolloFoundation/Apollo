/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public abstract class MonetarySystemExchangeAttachment extends AbstractAttachment implements MonetarySystemAttachment {

    final long currencyId;
    final long rateATM;
    final long units;

    public MonetarySystemExchangeAttachment(ByteBuffer buffer) {
        super(buffer);
        this.currencyId = buffer.getLong();
        this.rateATM = buffer.getLong();
        this.units = buffer.getLong();
    }

    public MonetarySystemExchangeAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.rateATM = Convert.parseLong(attachmentData.get("rateATM"));
        this.units = Convert.parseLong(attachmentData.get("units"));
    }

    public MonetarySystemExchangeAttachment(long currencyId, long rateATM, long units) {
        this.currencyId = currencyId;
        this.rateATM = rateATM;
        this.units = units;
    }

    @Override
    public int getMySize() {
        return 8 + 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
        buffer.putLong(rateATM);
        buffer.putLong(units);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("currency", Long.toUnsignedString(currencyId));
        attachment.put("rateATM", rateATM);
        attachment.put("units", units);
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

    public long getRateATM() {
        return rateATM;
    }

    public long getUnits() {
        return units;
    }

}
