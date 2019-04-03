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
public abstract class AbstractShufflingAttachment extends AbstractAttachment implements ShufflingAttachment {
    
    final long shufflingId;
    final byte[] shufflingStateHash;

    public AbstractShufflingAttachment(ByteBuffer buffer) {
        super(buffer);
        this.shufflingId = buffer.getLong();
        this.shufflingStateHash = new byte[32];
        buffer.get(this.shufflingStateHash);
    }

    public AbstractShufflingAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.shufflingId = Convert.parseUnsignedLong((String) attachmentData.get("shuffling"));
        this.shufflingStateHash = Convert.parseHexString((String) attachmentData.get("shufflingStateHash"));
    }

    public AbstractShufflingAttachment(long shufflingId, byte[] shufflingStateHash) {
        this.shufflingId = shufflingId;
        this.shufflingStateHash = shufflingStateHash;
    }

    @Override
    int getMySize() {
        return 8 + 32;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(shufflingId);
        buffer.put(shufflingStateHash);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("shuffling", Long.toUnsignedString(shufflingId));
        attachment.put("shufflingStateHash", Convert.toHexString(shufflingStateHash));
    }

    @Override
    public final long getShufflingId() {
        return shufflingId;
    }

    @Override
    public final byte[] getShufflingStateHash() {
        return shufflingStateHash;
    }
    
}
