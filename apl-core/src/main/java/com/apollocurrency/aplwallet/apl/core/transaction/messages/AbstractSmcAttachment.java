/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
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

    AbstractSmcAttachment(RlpReader reader) {
        super(reader);
        this.fuelLimit = reader.readBigInteger();
        this.fuelPrice = reader.readBigInteger();
    }

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
        this.fuelLimit = BigInteger.valueOf(((Number) attachmentData.get(FUEL_LIMIT_FIELD)).longValue());
        this.fuelPrice = BigInteger.valueOf(((Number) attachmentData.get(FUEL_PRICE_FIELD)).longValue());
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put(FUEL_LIMIT_FIELD, this.fuelLimit.longValueExact());
        json.put(FUEL_PRICE_FIELD, this.fuelPrice.longValueExact());
    }

    @Override
    public void putMyBytes(RlpList.RlpListBuilder builder) {
        builder
            .add(fuelLimit)
            .add(fuelPrice);
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
