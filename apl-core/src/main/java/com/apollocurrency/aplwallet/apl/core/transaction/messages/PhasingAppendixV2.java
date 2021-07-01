/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class PhasingAppendixV2 extends PhasingAppendix {

    /**
     * Time in seconds
     */
    private final int finishTime;

    public PhasingAppendixV2(ByteBuffer buffer) {
        super(buffer);
        finishTime = buffer.getInt();
    }

    public PhasingAppendixV2(JSONObject attachmentData) {
        super(attachmentData);
        Number phasingFinishTime = (Number) attachmentData.get("phasingFinishTime");

        this.finishTime = phasingFinishTime != null ? phasingFinishTime.intValue() : -1;
    }

    public PhasingAppendixV2(int finishHeight, int finishTime, PhasingParams phasingParams, byte[][] linkedFullHashes, byte[] hashedSecret, byte algorithm) {
        super(finishHeight, phasingParams, linkedFullHashes, hashedSecret, algorithm);
        this.finishTime = finishTime;
    }

    //TODO think it over how to change it (magic numbers).
    @Override
    public byte getVersion() {
        return Byte.valueOf("2");
    }

    //TODO think it over how to change it (magic numbers).
    @Override
    public String getAppendixName() {
        return "Phasing_V2";
    }

    @Override
    public int getMySize() {
        return super.getMySize() + 4;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);
        buffer.putInt(finishTime);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        super.putMyJSON(json);
        json.put("phasingFinishTime", finishTime);
    }

    @Override
    public void performFullValidation(Transaction transaction, int blockHeight) {
        throw new UnsupportedOperationException("Validate for PhasingV2 is not supported, use separate class");
    }

    public int getFinishTime() {
        return finishTime;
    }

}