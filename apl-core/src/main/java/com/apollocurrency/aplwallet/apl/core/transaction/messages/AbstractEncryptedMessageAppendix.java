/*
 * Copyright © 2018-2021 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

public abstract class AbstractEncryptedMessageAppendix extends AbstractAppendix {

    private final EncryptedData encryptedData;
    private final boolean isText;
    private final boolean isCompressed;

    public AbstractEncryptedMessageAppendix(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        int length = buffer.getInt();
        this.isText = length < 0;
        if (length < 0) {
            length &= Integer.MAX_VALUE;
        }
        try {
            this.encryptedData = EncryptedData.readEncryptedData(buffer, length, 1000);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
        this.isCompressed = getVersion() != 2;
    }

    public AbstractEncryptedMessageAppendix(JSONObject attachmentJSON, Map<?,?> encryptedMessageJSON) {
        super(attachmentJSON);
        byte[] data = Convert.parseHexString((String) encryptedMessageJSON.get("data"));
        byte[] nonce = Convert.parseHexString((String) encryptedMessageJSON.get("nonce"));
        this.encryptedData = new EncryptedData(data, nonce);
        this.isText = Boolean.TRUE.equals(encryptedMessageJSON.get("isText"));
        Object isCompressed = encryptedMessageJSON.get("isCompressed");
        this.isCompressed = isCompressed == null || Boolean.TRUE.equals(isCompressed);
    }

    public AbstractEncryptedMessageAppendix(EncryptedData encryptedData, boolean isText, boolean isCompressed) {
        super(isCompressed ? 1 : 2);
        this.encryptedData = encryptedData;
        this.isText = isText;
        this.isCompressed = isCompressed;
    }

    @Override
    public int getMySize() {
        return 4 + encryptedData.getSize();
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putInt(isText ? (encryptedData.getData().length | Integer.MIN_VALUE) : encryptedData.getData().length);
        buffer.put(encryptedData.getData());
        buffer.put(encryptedData.getNonce());
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("data", Convert.toHexString(encryptedData.getData()));
        json.put("nonce", Convert.toHexString(encryptedData.getNonce()));
        json.put("isText", isText);
        json.put("isCompressed", isCompressed);
    }

    @Override
    public Fee getBaselineFee(Transaction transaction, long oneAPL) {
        return new Fee.SizeBasedFee(oneAPL, oneAPL, 32) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                return ((AbstractEncryptedMessageAppendix) appendage).getEncryptedDataLength() - 16;
            }
        };
    }

    @Override
    public void performStateDependentValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        throw new UnsupportedOperationException("Validation for encrypted message appendix is not supported");
    }

    @Override
    public boolean verifyVersion() {
        return getVersion() == 1 || getVersion() == 2;
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
    }

    public final EncryptedData getEncryptedData() {
        return encryptedData;
    }

    int getEncryptedDataLength() {
        return encryptedData.getData().length;
    }

    public final boolean isText() {
        return isText;
    }

    public final boolean isCompressed() {
        return isCompressed;
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    public abstract String getAppendixName();

}