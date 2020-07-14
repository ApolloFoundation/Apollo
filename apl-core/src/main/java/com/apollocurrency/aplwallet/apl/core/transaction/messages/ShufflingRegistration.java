/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling.ShufflingTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class ShufflingRegistration extends AbstractAttachment implements ShufflingAttachment {

    final byte[] shufflingFullHash;

    public ShufflingRegistration(ByteBuffer buffer) {
        super(buffer);
        this.shufflingFullHash = new byte[32];
        buffer.get(this.shufflingFullHash);
    }

    public ShufflingRegistration(JSONObject attachmentData) {
        super(attachmentData);
        this.shufflingFullHash = Convert.parseHexString((String) attachmentData.get("shufflingFullHash"));
    }

    public ShufflingRegistration(byte[] shufflingFullHash) {
        this.shufflingFullHash = shufflingFullHash;
    }

    @Override
    public TransactionType getTransactionType() {
        return ShufflingTransactionType.SHUFFLING_REGISTRATION;
    }

    @Override
    public int getMySize() {
        return 32;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(shufflingFullHash);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("shufflingFullHash", Convert.toHexString(shufflingFullHash));
    }

    @Override
    public long getShufflingId() {
        return Convert.fullHashToId(shufflingFullHash);
    }

    @Override
    public byte[] getShufflingStateHash() {
        return shufflingFullHash;
    }

}
