/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public class UnencryptedEncryptedMessageAppendix extends EncryptedMessageAppendix implements Encryptable {

    private final byte[] messageToEncrypt;
    private final byte[] recipientPublicKey;

    public UnencryptedEncryptedMessageAppendix(JSONObject attachmentData) {
        super(attachmentData);
        setEncryptedData(null);
        JSONObject encryptedMessageJSON = (JSONObject)attachmentData.get("encryptedMessage");
        String messageToEncryptString = (String)encryptedMessageJSON.get("messageToEncrypt");
        messageToEncrypt = isText() ? Convert.toBytes(messageToEncryptString) : Convert.parseHexString(messageToEncryptString);
        recipientPublicKey = Convert.parseHexString((String)attachmentData.get("recipientPublicKey"));
    }

    public UnencryptedEncryptedMessageAppendix(byte[] messageToEncrypt, boolean isText, boolean isCompressed, byte[] recipientPublicKey) {
        super(null, isText, isCompressed);
        this.messageToEncrypt = messageToEncrypt;
        this.recipientPublicKey = recipientPublicKey;
    }

    @Override
    int getMySize() {
        if (getEncryptedData() != null) {
            return super.getMySize();
        }
        return 4 + EncryptedData.getEncryptedSize(getPlaintext());
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        if (getEncryptedData() == null) {
            throw new AplException.NotYetEncryptedException("Message not yet encrypted");
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
            json.put("encryptedMessage", encryptedMessageJSON);
            json.put("recipientPublicKey", Convert.toHexString(recipientPublicKey));
        } else {
            super.putMyJSON(json);
        }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (getEncryptedData() == null) {
            throw new AplException.NotYetEncryptedException("Message not yet encrypted");
        }
        super.apply(transaction, senderAccount, recipientAccount);
    }

    @Override
    public void encrypt(byte[] keySeed) {
        setEncryptedData(EncryptedData.encrypt(getPlaintext(), keySeed, recipientPublicKey));
    }

    private byte[] getPlaintext() {
        return isCompressed() && messageToEncrypt.length > 0 ? Convert.compress(messageToEncrypt) : messageToEncrypt;
    }

    @Override
    int getEncryptedDataLength() {
        return EncryptedData.getEncryptedDataLength(getPlaintext());
    }

}