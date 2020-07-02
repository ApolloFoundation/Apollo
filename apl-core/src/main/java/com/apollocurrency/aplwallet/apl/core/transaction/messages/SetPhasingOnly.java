/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.control.SetPhasingOnlyTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class SetPhasingOnly extends AbstractAttachment {

    final long maxFees;
    final short minDuration;
    final short maxDuration;
    private final PhasingParams phasingParams;

    public SetPhasingOnly(PhasingParams params, long maxFees, short minDuration, short maxDuration) {
        phasingParams = params;
        this.maxFees = maxFees;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    public SetPhasingOnly(ByteBuffer buffer) {
        super(buffer);
        phasingParams = new PhasingParams(buffer);
        maxFees = buffer.getLong();
        minDuration = buffer.getShort();
        maxDuration = buffer.getShort();
    }

    public SetPhasingOnly(JSONObject attachmentData) {
        super(attachmentData);
        JSONObject phasingControlParams = (JSONObject) attachmentData.get("phasingControlParams");
        phasingParams = new PhasingParams(phasingControlParams);
        maxFees = Convert.parseLong(attachmentData.get("controlMaxFees"));
        minDuration = ((Long) attachmentData.get("controlMinDuration")).shortValue();
        maxDuration = ((Long) attachmentData.get("controlMaxDuration")).shortValue();
    }

    @Override
    public TransactionType getTransactionType() {
        return SetPhasingOnlyTransactionType.SET_PHASING_ONLY;
    }

    @Override
    public int getMySize() {
        return phasingParams.getMySize() + 8 + 2 + 2;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        phasingParams.putMyBytes(buffer);
        buffer.putLong(maxFees);
        buffer.putShort(minDuration);
        buffer.putShort(maxDuration);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        JSONObject phasingControlParams = new JSONObject();
        phasingParams.putMyJSON(phasingControlParams);
        json.put("phasingControlParams", phasingControlParams);
        json.put("controlMaxFees", maxFees);
        json.put("controlMinDuration", minDuration);
        json.put("controlMaxDuration", maxDuration);
    }

    public PhasingParams getPhasingParams() {
        return phasingParams;
    }

    public long getMaxFees() {
        return maxFees;
    }

    public short getMinDuration() {
        return minDuration;
    }

    public short getMaxDuration() {
        return maxDuration;
    }

}
