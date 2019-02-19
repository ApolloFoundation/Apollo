/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
        this.rateATM = attachmentData.containsKey("rateATM") ? Convert.parseLong(attachmentData.get("rateATM")) : Convert.parseLong(attachmentData.get("rateNQT"));
        this.units = Convert.parseLong(attachmentData.get("units"));
    }

    public MonetarySystemExchangeAttachment(long currencyId, long rateATM, long units) {
        this.currencyId = currencyId;
        this.rateATM = rateATM;
        this.units = units;
    }

    @Override
    int getMySize() {
        return 8 + 8 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
        buffer.putLong(rateATM);
        buffer.putLong(units);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
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
