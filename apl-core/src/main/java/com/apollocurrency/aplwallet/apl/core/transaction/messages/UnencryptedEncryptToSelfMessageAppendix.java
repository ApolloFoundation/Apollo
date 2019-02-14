/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public class UnencryptedEncryptToSelfMessageAppendix extends EncryptToSelfMessageAppendix implements Encryptable {

    private final byte[] messageToEncrypt;

    public UnencryptedEncryptToSelfMessageAppendix(JSONObject attachmentData) {
        super(attachmentData);
        setEncryptedData(null);
        JSONObject encryptedMessageJSON = (JSONObject)attachmentData.get("encryptToSelfMessage");
        String messageToEncryptString = (String)encryptedMessageJSON.get("messageToEncrypt");
        messageToEncrypt = isText() ? Convert.toBytes(messageToEncryptString) : Convert.parseHexString(messageToEncryptString);
    }

    public UnencryptedEncryptToSelfMessageAppendix(byte[] messageToEncrypt, boolean isText, boolean isCompressed) {
        super(null, isText, isCompressed);
        this.messageToEncrypt = messageToEncrypt;
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
            json.put("encryptToSelfMessage", encryptedMessageJSON);
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
        setEncryptedData(EncryptedData.encrypt(getPlaintext(), keySeed, Crypto.getPublicKey(keySeed)));
    }

    @Override
    int getEncryptedDataLength() {
        return EncryptedData.getEncryptedDataLength(getPlaintext());
    }

    private byte[] getPlaintext() {
        return isCompressed() && messageToEncrypt.length > 0 ? Convert.compress(messageToEncrypt) : messageToEncrypt;
    }

}