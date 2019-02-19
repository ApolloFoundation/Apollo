/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
        return ShufflingTransaction.SHUFFLING_REGISTRATION;
    }

    @Override
    int getMySize() {
        return 32;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.put(shufflingFullHash);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
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
