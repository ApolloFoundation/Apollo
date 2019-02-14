/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.TaggedData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Data;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import javax.enterprise.inject.spi.CDI;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class TaggedDataExtend extends TaggedDataAttachment {
    
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

    public static TaggedDataExtend parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(Data.TAGGED_DATA_EXTEND.getName(), attachmentData)) {
            return null;
        }
        return new TaggedDataExtend(attachmentData);
    }
    private volatile byte[] hash;
    final long taggedDataId;
    final boolean jsonIsPruned;

    public TaggedDataExtend(ByteBuffer buffer) {
        super(buffer);
        this.taggedDataId = buffer.getLong();
        this.jsonIsPruned = false;
    }

    public TaggedDataExtend(JSONObject attachmentData) {
        super(attachmentData);
        this.taggedDataId = Convert.parseUnsignedLong((String) attachmentData.get("taggedData"));
        this.jsonIsPruned = attachmentData.get("data") == null;
    }

    public TaggedDataExtend(TaggedData taggedData) {
        super(taggedData.getName(), taggedData.getDescription(), taggedData.getTags(), taggedData.getType(), taggedData.getChannel(), taggedData.isText(), taggedData.getFilename(), taggedData.getData());
        this.taggedDataId = taggedData.getId();
        this.jsonIsPruned = false;
    }

    @Override
    int getMySize() {
        return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(taggedDataId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        attachment.put("taggedData", Long.toUnsignedString(taggedDataId));
    }

    @Override
    public TransactionType getTransactionType() {
        return Data.TAGGED_DATA_EXTEND;
    }

    public long getTaggedDataId() {
        return taggedDataId;
    }

    @Override
    public byte[] getHash() {
        if (hash == null) {
            hash = super.getHash();
        }
        if (hash == null) {
            TaggedDataUpload taggedDataUpload = (TaggedDataUpload) blockchain.getTransaction(taggedDataId).getAttachment();
            hash = taggedDataUpload.getHash();
        }
        return hash;
    }

    @Override
    long getTaggedDataId(Transaction transaction) {
        return taggedDataId;
    }

    public boolean jsonIsPruned() {
        return jsonIsPruned;
    }

    @Override
    public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
    }
    
}
