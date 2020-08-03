/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling.ShufflingProcessingTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * @author al
 */
public final class ShufflingProcessingAttachment extends AbstractShufflingAttachment implements Prunable {

    private static final byte[] emptyDataHash = Crypto.sha256().digest();
    final byte[] hash;
    volatile byte[][] data;

    public ShufflingProcessingAttachment(ByteBuffer buffer) {
        super(buffer);
        this.hash = new byte[32];
        buffer.get(hash);
        this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
    }

    public ShufflingProcessingAttachment(JSONObject attachmentData) {
        super(attachmentData);
        JSONArray jsonArray = (JSONArray) attachmentData.get("data");
        if (jsonArray != null) {
            this.data = new byte[jsonArray.size()][];
            for (int i = 0; i < this.data.length; i++) {
                this.data[i] = Convert.parseHexString((String) jsonArray.get(i));
            }
            this.hash = null;
        } else {
            this.hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("hash")));
            this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
        }
    }

    public ShufflingProcessingAttachment(long shufflingId, byte[][] data, byte[] shufflingStateHash) {
        super(shufflingId, shufflingStateHash);
        this.data = data;
        this.hash = null;
    }

    public static ShufflingProcessingAttachment parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(ShufflingProcessingTransactionType.NAME, attachmentData)) {
            return null;
        }
        return new ShufflingProcessingAttachment(attachmentData);
    }

    @Override
    public int getMyFullSize() {
        int size = super.getMySize();
        if (data != null) {
            size += 1;
            for (byte[] bytes : data) {
                size += 4;
                size += bytes.length;
            }
        }
        return size / 2; // just lie
    }

    @Override
    public int getMySize() {
        return super.getMySize() + 32;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);
        buffer.put(getHash());
    }

    public void setData(byte[][] data) {
        this.data = data;
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        if (data != null) {
            JSONArray jsonArray = new JSONArray();
            attachment.put("data", jsonArray);
            for (byte[] bytes : data) {
                jsonArray.add(Convert.toHexString(bytes));
            }
        }
        attachment.put("hash", Convert.toHexString(getHash()));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SHUFFLING_PROCESSING;
    }

    @Override
    public byte[] getHash() {
        if (hash != null) {
            return hash;
        } else if (data != null) {
            MessageDigest digest = Crypto.sha256();
            for (byte[] bytes : data) {
                digest.update(bytes);
            }
            return digest.digest();
        } else {
            throw new IllegalStateException("Both hash and data are null");
        }
    }

    public byte[][] getData() {
        return data;
    }

    @Override
    public boolean hasPrunableData() {
        return data != null;
    }

}
