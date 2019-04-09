/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

import static org.slf4j.LoggerFactory.getLogger;

public class PhasingAppendixV2 extends PhasingAppendix {
    private static final Logger LOG = getLogger(PhasingAppendixV2.class);
    public static final String appendixName = "Phasing_V2";

    public static final byte VERSION = 2;


    public static PhasingAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        return new PhasingAppendix(attachmentData);
    }

    private final int finishTime;

    public PhasingAppendixV2(ByteBuffer buffer) {
        this(buffer, VERSION);
    }

    public PhasingAppendixV2(ByteBuffer buffer, byte version) {
        super(buffer, version);
        finishTime = buffer.getInt();
    }

    public PhasingAppendixV2(JSONObject attachmentData) {
        super(attachmentData);
        Long phasingFinishTime = (Long) attachmentData.get("phasingFinishTime");

        this.finishTime = phasingFinishTime != null ? phasingFinishTime.intValue() : -1;
    }

    public PhasingAppendixV2(int finishHeight, int finishTime, PhasingParams phasingParams, byte[][] linkedFullHashes, byte[] hashedSecret, byte algorithm) {
        super(finishHeight, phasingParams, linkedFullHashes, hashedSecret, algorithm);
        this.finishTime = finishTime;
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    int getMySize() {
        return super.getMySize() + 4;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);
        buffer.putInt(finishTime);
    }

    @Override
    void putMyJSON(JSONObject json) {
        super.putMyJSON(json);
        json.put("phasingFinishTime", finishTime);
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
       super.generalValidation(transaction);

       validateFinishHeightAndTime(blockHeight, this.getFinishTime());
    }

    public int getFinishTime() {
        return finishTime;
    }

    @Override
    public byte getVERSION() {
        return VERSION;
    }
}