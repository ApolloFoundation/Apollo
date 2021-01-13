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
import java.util.ArrayList;
import java.util.List;

/**
 * @author al
 */
public final class MessagingPhasingVoteCasting extends AbstractAttachment {

    final List<byte[]> transactionFullHashes;
    final byte[] revealedSecret;

    public MessagingPhasingVoteCasting(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        byte length = buffer.get();
        transactionFullHashes = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            byte[] hash = new byte[32];
            buffer.get(hash);
            transactionFullHashes.add(hash);
        }
        int secretLength = buffer.getInt();
        if (secretLength > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
            throw new AplException.NotValidException("Invalid revealed secret length " + secretLength);
        }
        if (secretLength > 0) {
            revealedSecret = new byte[secretLength];
            buffer.get(revealedSecret);
        } else {
            revealedSecret = Convert.EMPTY_BYTE;
        }
    }

    public MessagingPhasingVoteCasting(JSONObject attachmentData) {
        super(attachmentData);
        List<?> hashes = (List<?>) attachmentData.get("transactionFullHashes");
        transactionFullHashes = new ArrayList<>(hashes.size());
        hashes.forEach((hash) -> transactionFullHashes.add(Convert.parseHexString((String) hash)));
        String revealedSecret = Convert.emptyToNull((String) attachmentData.get("revealedSecret"));
        this.revealedSecret = revealedSecret != null ? Convert.parseHexString(revealedSecret) : Convert.EMPTY_BYTE;
    }

    public MessagingPhasingVoteCasting(List<byte[]> transactionFullHashes, byte[] revealedSecret) {
        this.transactionFullHashes = transactionFullHashes;
        this.revealedSecret = revealedSecret;
    }

    @Override
    public int getMySize() {
        return 1 + 32 * transactionFullHashes.size() + 4 + revealedSecret.length;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put((byte) transactionFullHashes.size());
        transactionFullHashes.forEach(buffer::put);
        buffer.putInt(revealedSecret.length);
        buffer.put(revealedSecret);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        JSONArray jsonArray = new JSONArray();
        transactionFullHashes.forEach((hash) -> jsonArray.add(Convert.toHexString(hash)));
        attachment.put("transactionFullHashes", jsonArray);
        if (revealedSecret.length > 0) {
            attachment.put("revealedSecret", Convert.toHexString(revealedSecret));
        }
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.PHASING_VOTE_CASTING;
    }

    public List<byte[]> getTransactionFullHashes() {
        return transactionFullHashes;
    }

    public byte[] getRevealedSecret() {
        return revealedSecret;
    }

}
