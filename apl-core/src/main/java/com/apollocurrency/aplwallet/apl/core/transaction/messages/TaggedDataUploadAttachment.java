/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.types.data.TaggedDataUploadTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author al
 */
public final class TaggedDataUploadAttachment extends TaggedDataAttachment {

    final byte[] hash;

    public TaggedDataUploadAttachment(ByteBuffer buffer) {
        super(buffer);
        this.hash = new byte[32];
        buffer.get(hash);
    }

    public TaggedDataUploadAttachment(JSONObject attachmentData) {
        super(attachmentData);
        String dataJSON = (String) attachmentData.get("data");
        if (dataJSON == null) {
            this.hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("hash")));
        } else {
            this.hash = null;
        }
    }

    public TaggedDataUploadAttachment(String name, String description, String tags, String type, String channel,
                                      boolean isText, String filename, byte[] data) throws AplException.NotValidException {
        super(name, description, tags, type, channel, isText, filename, data);
        this.hash = null;
        if (isText && !Arrays.equals(data, Convert.toBytes(Convert.toString(data)))) {
            throw new AplException.NotValidException("Data is not UTF-8 text");
        }
    }

    public static TaggedDataUploadAttachment parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(TaggedDataUploadTransactionType.NAME, attachmentData)) {
            return null;
        }
        return new TaggedDataUploadAttachment(attachmentData);
    }

    @Override
    public int getMySize() {
        return 32;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(getHash());
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        attachment.put("hash", Convert.toHexString(getHash()));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.TAGGED_DATA_UPLOAD;
    }

    @Override
    public byte[] getHash() {
        if (hash != null) {
            return hash;
        }
        return super.getHash();
    }

    @Override
    public long getTaggedDataId(Transaction transaction) {
        return transaction.getId();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TaggedDataUploadAttachment{");
        super.printCommonFields(sb);
        sb.append('}');
        return sb.toString();
    }
}
