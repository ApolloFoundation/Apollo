/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class SmcAbstractAttachment extends AbstractAttachment {

    public SmcAbstractAttachment(ByteBuffer buffer) {
        super(buffer);
    }

    public SmcAbstractAttachment(RlpReader reader) {
        super(reader);
    }

    public SmcAbstractAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public SmcAbstractAttachment(int version) {
        super(version);
    }

    public SmcAbstractAttachment() {
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public byte getVersion() {
        return 1;
    }

    @Override
    public int getMySize() {
        return -1;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        throw new UnsupportedOperationException("Unsupported operation since TransactionV3, use RLP writer instead of.");
    }
}
