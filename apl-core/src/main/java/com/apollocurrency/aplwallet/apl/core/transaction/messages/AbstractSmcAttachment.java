/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public abstract class AbstractSmcAttachment extends AbstractAttachment {
    protected static final String AMOUNT_FIELD = "amount";
    protected static final String FUEL_LIMIT_FIELD = "fuelLimit";
    protected static final String FUEL_PRICE_FIELD = "fuelPrice";

    @Getter
    private final BigInteger fuelLimit;
    @Getter
    private final BigInteger fuelPrice;

    public AbstractSmcAttachment(BigInteger fuelLimit, BigInteger fuelPrice) {
        super();
        this.fuelLimit = fuelLimit;
        this.fuelPrice = fuelPrice;
    }

    public AbstractSmcAttachment(ByteBuffer buffer) {
        super(buffer);
        this.fuelLimit = BigInteger.valueOf(buffer.getLong());
        this.fuelPrice = BigInteger.valueOf(buffer.getLong());
    }

    AbstractSmcAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.fuelLimit = BigInteger.valueOf(Convert.parseLong(attachmentData.get(FUEL_LIMIT_FIELD)));
        this.fuelPrice = BigInteger.valueOf(Convert.parseLong(attachmentData.get(FUEL_PRICE_FIELD)));
    }

    //TODO ??? what about string compressing, something like: output = Compressor.deflate(input)
    public abstract int getPayableSize();

    @Override
    public void putMyJSON(JSONObject json) {
        json.put(FUEL_LIMIT_FIELD, this.fuelLimit.longValueExact());
        json.put(FUEL_PRICE_FIELD, this.fuelPrice.longValueExact());
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(fuelLimit.longValueExact());
        buffer.putLong(fuelPrice.longValueExact());
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public byte getVersion() {
        return 1;
    }
}
