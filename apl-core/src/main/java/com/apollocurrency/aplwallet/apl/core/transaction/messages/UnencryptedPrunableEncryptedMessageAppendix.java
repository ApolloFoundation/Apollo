/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public class UnencryptedPrunableEncryptedMessageAppendix extends PrunableEncryptedMessageAppendix implements Encryptable {

    private final byte[] messageToEncrypt;
    private final byte[] recipientPublicKey;

    public UnencryptedPrunableEncryptedMessageAppendix(JSONObject attachmentJSON) {
        super(attachmentJSON);
        setEncryptedData(null);
        JSONObject encryptedMessageJSON = (JSONObject)attachmentJSON.get("encryptedMessage");
        String messageToEncryptString = (String)encryptedMessageJSON.get("messageToEncrypt");
        this.messageToEncrypt = isText() ? Convert.toBytes(messageToEncryptString) : Convert.parseHexString(messageToEncryptString);
        this.recipientPublicKey = Convert.parseHexString((String)attachmentJSON.get("recipientPublicKey"));
    }

    public UnencryptedPrunableEncryptedMessageAppendix(byte[] messageToEncrypt, boolean isText, boolean isCompressed, byte[] recipientPublicKey) {
        super(null, isText, isCompressed);
        this.messageToEncrypt = messageToEncrypt;
        this.recipientPublicKey = recipientPublicKey;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        if (getEncryptedData() == null) {
            throw new AplException.NotYetEncryptedException("Prunable encrypted message not yet encrypted");
        }
        super.putMyBytes(buffer);
    }

    @Override
    void putMyJSON(JSONObject json) {
        if (getEncryptedData() == null) {
            JSONObject encryptedMessageJSON = new JSONObject();
            encryptedMessageJSON.put("messageToEncrypt", isText() ? Convert.toString(messageToEncrypt) : Convert.toHexString(messageToEncrypt));
            encryptedMessageJSON.put("isText", isText());
            encryptedMessageJSON.put("isCompressed", isCompressed());
            json.put("recipientPublicKey", Convert.toHexString(recipientPublicKey));
            json.put("encryptedMessage", encryptedMessageJSON);
        } else {
            super.putMyJSON(json);
        }
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (getEncryptedData() == null) {
            int dataLength = getEncryptedDataLength();
            if (dataLength > Constants.MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH) {
                throw new AplException.NotValidException(String.format("Message length %d exceeds max prunable encrypted message length %d",
                        dataLength, Constants.MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH));
            }
        } else {
            super.validate(transaction, blockHeight);
        }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (getEncryptedData() == null) {
            throw new AplException.NotYetEncryptedException("Prunable encrypted message not yet encrypted");
        }
        super.apply(transaction, senderAccount, recipientAccount);
    }

    @Override
    public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {}

    @Override
    public void encrypt(byte[] keySeed) {
        setEncryptedData(EncryptedData.encrypt(getPlaintext(), keySeed, recipientPublicKey));
    }

    @Override
    int getEncryptedDataLength() {
        return EncryptedData.getEncryptedDataLength(getPlaintext());
    }

    private byte[] getPlaintext() {
        return isCompressed() && messageToEncrypt.length > 0 ? Convert.compress(messageToEncrypt) : messageToEncrypt;
    }

}