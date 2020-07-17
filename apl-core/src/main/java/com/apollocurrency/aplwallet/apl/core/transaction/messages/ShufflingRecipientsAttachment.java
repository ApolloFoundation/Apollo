/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class ShufflingRecipientsAttachment extends AbstractShufflingAttachment {

    final byte[][] recipientPublicKeys;

    public ShufflingRecipientsAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        int count = buffer.get();
        if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count < 0) {
            throw new AplException.NotValidException("Invalid data count " + count);
        }
        this.recipientPublicKeys = new byte[count][];
        for (int i = 0; i < count; i++) {
            this.recipientPublicKeys[i] = new byte[32];
            buffer.get(this.recipientPublicKeys[i]);
        }
    }

    public ShufflingRecipientsAttachment(JSONObject attachmentData) {
        super(attachmentData);
        JSONArray jsonArray = (JSONArray) attachmentData.get("recipientPublicKeys");
        this.recipientPublicKeys = new byte[jsonArray.size()][];
        for (int i = 0; i < this.recipientPublicKeys.length; i++) {
            this.recipientPublicKeys[i] = Convert.parseHexString((String) jsonArray.get(i));
        }
    }

    public ShufflingRecipientsAttachment(long shufflingId, byte[][] recipientPublicKeys, byte[] shufflingStateHash) {
        super(shufflingId, shufflingStateHash);
        this.recipientPublicKeys = recipientPublicKeys;
    }

    @Override
    public int getMySize() {
        int size = super.getMySize();
        size += 1;
        size += 32 * recipientPublicKeys.length;
        return size;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);
        buffer.put((byte) recipientPublicKeys.length);
        for (byte[] bytes : recipientPublicKeys) {
            buffer.put(bytes);
        }
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        JSONArray jsonArray = new JSONArray();
        attachment.put("recipientPublicKeys", jsonArray);
        for (byte[] bytes : recipientPublicKeys) {
            jsonArray.add(Convert.toHexString(bytes));
        }
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionType() {
        return TransactionTypes.TransactionTypeSpec.SHUFFLING_RECIPIENTS;
    }

    public byte[][] getRecipientPublicKeys() {
        return recipientPublicKeys;
    }

}
