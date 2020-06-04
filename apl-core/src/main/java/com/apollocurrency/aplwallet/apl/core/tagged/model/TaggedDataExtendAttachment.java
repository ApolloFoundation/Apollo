/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataAttachment;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.transaction.Data;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class TaggedDataExtendAttachment extends TaggedDataAttachment {

    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    final long taggedDataId;
    final boolean jsonIsPruned;
    private volatile byte[] hash;

    public TaggedDataExtendAttachment(ByteBuffer buffer) {
        super(buffer);
        this.taggedDataId = buffer.getLong();
        this.jsonIsPruned = false;
    }

    public TaggedDataExtendAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.taggedDataId = Convert.parseUnsignedLong((String) attachmentData.get("taggedData"));
        this.jsonIsPruned = attachmentData.get("data") == null;
    }

    public TaggedDataExtendAttachment(TaggedData taggedData) {
        super(taggedData.getName(), taggedData.getDescription(), taggedData.getTags(), taggedData.getType(), taggedData.getChannel(), taggedData.isText(), taggedData.getFilename(), taggedData.getData());
        this.taggedDataId = taggedData.getId();
        this.jsonIsPruned = false;
    }

    public static TaggedDataExtendAttachment parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(Data.TAGGED_DATA_EXTEND.getName(), attachmentData)) {
            return null;
        }
        return new TaggedDataExtendAttachment(attachmentData);
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(taggedDataId);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
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
            TaggedDataUploadAttachment taggedDataUploadAttachment = (TaggedDataUploadAttachment) blockchain.getTransaction(taggedDataId).getAttachment();
            hash = taggedDataUploadAttachment.getHash();
        }
        return hash;
    }

    @Override
    public long getTaggedDataId(Transaction transaction) {
        return taggedDataId;
    }

    public boolean jsonIsPruned() {
        return jsonIsPruned;
    }

    @Override
    public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TaggedDataExtendAttachment{");
        sb.append("taggedDataId=").append(taggedDataId);
        super.printCommonFields(sb);
        sb.append('}');
        return sb.toString();
    }
}
