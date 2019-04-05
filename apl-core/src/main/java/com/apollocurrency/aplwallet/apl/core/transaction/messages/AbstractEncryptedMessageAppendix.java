/*
 * Copyright © 2018-2019 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public abstract class AbstractEncryptedMessageAppendix extends AbstractAppendix {

    private static final Fee ENCRYPTED_MESSAGE_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL, 32) {
        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            return ((AbstractEncryptedMessageAppendix)appendage).getEncryptedDataLength() - 16;
        }
    };

    private EncryptedData encryptedData;
    private boolean isText;
    private boolean isCompressed;

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

    public AbstractEncryptedMessageAppendix(JSONObject attachmentJSON, JSONObject encryptedMessageJSON) {
        super(attachmentJSON);
        byte[] data = Convert.parseHexString((String)encryptedMessageJSON.get("data"));
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
    int getMySize() {
        return 4 + encryptedData.getSize();
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putInt(isText ? (encryptedData.getData().length | Integer.MIN_VALUE) : encryptedData.getData().length);
        buffer.put(encryptedData.getData());
        buffer.put(encryptedData.getNonce());
    }

    @Override
    void putMyJSON(JSONObject json) {
        json.put("data", Convert.toHexString(encryptedData.getData()));
        json.put("nonce", Convert.toHexString(encryptedData.getNonce()));
        json.put("isText", isText);
        json.put("isCompressed", isCompressed);
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return ENCRYPTED_MESSAGE_FEE;
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (getEncryptedDataLength() > Constants.MAX_ENCRYPTED_MESSAGE_LENGTH) {
            throw new AplException.NotValidException("Max encrypted message length exceeded");
        }
        if (encryptedData != null) {
            if ((encryptedData.getNonce().length != 32 && encryptedData.getData().length > 0)
                    || (encryptedData.getNonce().length != 0 && encryptedData.getData().length == 0)) {
                throw new AplException.NotValidException("Invalid nonce length " + encryptedData.getNonce().length);
            }
        }
        if ((getVersion() != 2 && !isCompressed) || (getVersion() == 2 && isCompressed)) {
            throw new AplException.NotValidException("Version mismatch - version " + getVersion() + ", isCompressed " + isCompressed);
        }
    }

    @Override
    public boolean verifyVersion() {
        return getVersion() == 1 || getVersion() == 2;
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

    public final EncryptedData getEncryptedData() {
        return encryptedData;
    }

    final void setEncryptedData(EncryptedData encryptedData) {
        this.encryptedData = encryptedData;
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