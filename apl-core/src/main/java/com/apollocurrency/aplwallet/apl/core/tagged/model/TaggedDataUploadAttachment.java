/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataAttachment;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.transaction.types.data.DataTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
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
        if (!Appendix.hasAppendix(DataTransactionType.TAGGED_DATA_UPLOAD.getName(), attachmentData)) {
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
    public TransactionType getTransactionType() {
        return DataTransactionType.TAGGED_DATA_UPLOAD;
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
    public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        TaggedDataService taggedDataService = CDI.current().select(TaggedDataService.class).get();
        taggedDataService.restore(transaction, this, blockTimestamp, height);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TaggedDataUploadAttachment{");
        super.printCommonFields(sb);
        sb.append('}');
        return sb.toString();
    }
}
