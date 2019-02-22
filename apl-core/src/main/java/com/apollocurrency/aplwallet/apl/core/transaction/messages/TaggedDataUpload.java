/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.TaggedData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Data;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class TaggedDataUpload extends TaggedDataAttachment {
    
    public static TaggedDataUpload parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(Data.TAGGED_DATA_UPLOAD.getName(), attachmentData)) {
            return null;
        }
        return new TaggedDataUpload(attachmentData);
    }
    final byte[] hash;

    public TaggedDataUpload(ByteBuffer buffer) {
        super(buffer);
        this.hash = new byte[32];
        buffer.get(hash);
    }

    public TaggedDataUpload(JSONObject attachmentData) {
        super(attachmentData);
        String dataJSON = (String) attachmentData.get("data");
        if (dataJSON == null) {
            this.hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("hash")));
        } else {
            this.hash = null;
        }
    }

    public TaggedDataUpload(String name, String description, String tags, String type, String channel, boolean isText, String filename, byte[] data) throws AplException.NotValidException {
        super(name, description, tags, type, channel, isText, filename, data);
        this.hash = null;
        if (isText && !Arrays.equals(data, Convert.toBytes(Convert.toString(data)))) {
            throw new AplException.NotValidException("Data is not UTF-8 text");
        }
    }

    @Override
    int getMySize() {
        return 32;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.put(getHash());
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        attachment.put("hash", Convert.toHexString(getHash()));
    }

    @Override
    public TransactionType getTransactionType() {
        return Data.TAGGED_DATA_UPLOAD;
    }

    @Override
    public byte[] getHash() {
        if (hash != null) {
            return hash;
        }
        return super.getHash();
    }

    @Override
    long getTaggedDataId(Transaction transaction) {
        return transaction.getId();
    }

    @Override
    public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        TaggedData.restore(transaction, this, blockTimestamp, height);
    }
    
}
