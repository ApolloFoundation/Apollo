/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.types.data.TaggedDataExtendTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@Slf4j
public final class TaggedDataExtendAttachment extends TaggedDataAttachment {

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
        if (!Appendix.hasAppendix(TaggedDataExtendTransactionType.NAME, attachmentData)) {
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
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.TAGGED_DATA_EXTEND;
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
            log.warn("Hash can be extracted from blockchain, but for now this ability was suspended {}", ThreadUtils.last5Stacktrace());
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
    public String toString() {
        final StringBuffer sb = new StringBuffer("TaggedDataExtendAttachment{");
        sb.append("taggedDataId=").append(taggedDataId);
        super.printCommonFields(sb);
        sb.append('}');
        return sb.toString();
    }
}
