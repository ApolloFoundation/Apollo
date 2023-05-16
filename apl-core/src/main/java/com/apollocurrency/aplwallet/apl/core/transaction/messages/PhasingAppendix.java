/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.List;

public class PhasingAppendix extends AbstractAppendix {

    private final int finishHeight;
    private final PhasingParams params;
    private final byte[][] linkedFullHashes;
    private final byte[] hashedSecret;
    private final byte algorithm;


    public PhasingAppendix(ByteBuffer buffer) {
        finishHeight = buffer.getInt();
        params = new PhasingParams(buffer);

        byte linkedFullHashesSize = buffer.get();
        if (linkedFullHashesSize > 0) {
            linkedFullHashes = new byte[linkedFullHashesSize][];
            for (int i = 0; i < linkedFullHashesSize; i++) {
                linkedFullHashes[i] = new byte[32];
                buffer.get(linkedFullHashes[i]);
            }
        } else {
            linkedFullHashes = Convert.EMPTY_BYTES;
        }
        byte hashedSecretLength = buffer.get();
        if (hashedSecretLength > 0) {
            hashedSecret = new byte[hashedSecretLength];
            buffer.get(hashedSecret);
        } else {
            hashedSecret = Convert.EMPTY_BYTE;
        }
        algorithm = buffer.get();
    }

    public PhasingAppendix(JSONObject attachmentData) {
        super(attachmentData);
        Number phasingFinishHeight = (Number) attachmentData.get("phasingFinishHeight");

        this.finishHeight = phasingFinishHeight != null ? phasingFinishHeight.intValue() : -1;
        params = new PhasingParams(attachmentData);
        List<?> linkedFullHashesJson = (List<?>) attachmentData.get("phasingLinkedFullHashes");
        if (linkedFullHashesJson != null && linkedFullHashesJson.size() > 0) {
            linkedFullHashes = new byte[linkedFullHashesJson.size()][];
            for (int i = 0; i < linkedFullHashes.length; i++) {
                linkedFullHashes[i] = Convert.parseHexString((String) linkedFullHashesJson.get(i));
            }
        } else {
            linkedFullHashes = Convert.EMPTY_BYTES;
        }
        String hashedSecret = Convert.emptyToNull((String) attachmentData.get("phasingHashedSecret"));
        if (hashedSecret != null) {
            this.hashedSecret = Convert.parseHexString(hashedSecret);
            this.algorithm = ((Number) attachmentData.get("phasingHashedSecretAlgorithm")).byteValue();
        } else {
            this.hashedSecret = Convert.EMPTY_BYTE;
            this.algorithm = 0;
        }
    }

    public PhasingAppendix(int finishHeight, PhasingParams phasingParams, byte[][] linkedFullHashes, byte[] hashedSecret, byte algorithm) {
        this.finishHeight = finishHeight;
        this.params = phasingParams;
        this.linkedFullHashes = Convert.nullToEmpty(linkedFullHashes);
        this.hashedSecret = hashedSecret != null ? hashedSecret : Convert.EMPTY_BYTE;
        this.algorithm = algorithm;
    }

    //TODO think it over how to change it (magic numbers).
    @Override
    public String getAppendixName() {
        return "Phasing";
    }

    //TODO think it over how to change it (magic numbers).
    @Override
    public byte getVersion() {
        return 1;
    }

    @Override
    public int getMySize() {
        return 4 + params.getMySize() + 1 + 32 * linkedFullHashes.length + 1 + hashedSecret.length + 1;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putInt(finishHeight);
        params.putMyBytes(buffer);
        buffer.put((byte) linkedFullHashes.length);
        for (byte[] hash : linkedFullHashes) {
            buffer.put(hash);
        }
        buffer.put((byte) hashedSecret.length);
        buffer.put(hashedSecret);
        buffer.put(algorithm);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("phasingFinishHeight", finishHeight);
        params.putMyJSON(json);
        if (linkedFullHashes.length > 0) {
            JSONArray linkedFullHashesJson = new JSONArray();
            for (byte[] hash : linkedFullHashes) {
                linkedFullHashesJson.add(Convert.toHexString(hash));
            }
            json.put("phasingLinkedFullHashes", linkedFullHashesJson);
        }
        if (hashedSecret.length > 0) {
            json.put("phasingHashedSecret", Convert.toHexString(hashedSecret));
            json.put("phasingHashedSecretAlgorithm", algorithm);
        }
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public Fee getBaselineFee(Transaction tx, final long oneAPL) {
        return (transaction, appendage) -> {
            long fee = 0;
            PhasingAppendix phasing = (PhasingAppendix) appendage;
            if (!phasing.params.getVoteWeighting().isBalanceIndependent()) {
                fee += 20 * oneAPL;
            } else {
                fee += oneAPL;
            }
            if (phasing.hashedSecret.length > 0) {
                fee += (1 + (phasing.hashedSecret.length - 1) / 32) * oneAPL;
            }
            fee += oneAPL * phasing.linkedFullHashes.length;
            return fee;
        };
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        throw new UnsupportedOperationException("Apply for phasing appendix is not supported, use separate class");
    }

    @Override
    public void performStateDependentValidation(Transaction transaction, int blockHeight) {
        throw new UnsupportedOperationException("Validation for phasing appendix is not supported, use separate class");
    }

    @Override
    public void performStateIndependentValidation(Transaction transaction, int blockHeight) {
        throw new UnsupportedOperationException("Validation for phasing appendix is not supported, use separate validator");
    }

    public int getFinishHeight() {
        return finishHeight;
    }

    public long getQuorum() {
        return params.getQuorum();
    }

    public long[] getWhitelist() {
        return params.getWhitelist();
    }

    public VoteWeighting getVoteWeighting() {
        return params.getVoteWeighting();
    }

    public byte[][] getLinkedFullHashes() {
        return linkedFullHashes;
    }

    public byte[] getHashedSecret() {
        return hashedSecret;
    }

    public byte getAlgorithm() {
        return algorithm;
    }

    public PhasingParams getParams() {
        return params;
    }

    @Override
    public int getAppendixFlag() {
        return 0x10;
    }

}