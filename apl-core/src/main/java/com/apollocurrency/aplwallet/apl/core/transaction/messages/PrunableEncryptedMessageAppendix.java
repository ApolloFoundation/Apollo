/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import javax.enterprise.inject.spi.CDI;

public class PrunableEncryptedMessageAppendix extends AbstractAppendix implements Prunable {

    private static final String appendixName = "PrunableEncryptedMessage";
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();

    private static final Fee PRUNABLE_ENCRYPTED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_APL/10) {
        @Override
        public int getSize(Transaction transaction, Appendix appendix) {
            return appendix.getFullSize();
        }
    };

    public static PrunableEncryptedMessageAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        JSONObject encryptedMessageJSON = (JSONObject)attachmentData.get("encryptedMessage");
        if (encryptedMessageJSON != null && encryptedMessageJSON.get("data") == null) {
            return new UnencryptedPrunableEncryptedMessageAppendix(attachmentData);
        }
        return new PrunableEncryptedMessageAppendix(attachmentData);
    }

    private final byte[] hash;
    private EncryptedData encryptedData;
    private final boolean isText;
    private final boolean isCompressed;
    private volatile PrunableMessage prunableMessage;

    public PrunableEncryptedMessageAppendix(ByteBuffer buffer) {
        super(buffer);
        this.hash = new byte[32];
        buffer.get(this.hash);
        this.encryptedData = null;
        this.isText = false;
        this.isCompressed = false;
    }

    public PrunableEncryptedMessageAppendix(JSONObject attachmentJSON) {
        super(attachmentJSON);
        String hashString = Convert.emptyToNull((String) attachmentJSON.get("encryptedMessageHash"));
        JSONObject encryptedMessageJSON = (JSONObject) attachmentJSON.get("encryptedMessage");
        if (hashString != null && encryptedMessageJSON == null) {
            this.hash = Convert.parseHexString(hashString);
            this.encryptedData = null;
            this.isText = false;
            this.isCompressed = false;
        } else {
            this.hash = null;
            byte[] data = Convert.parseHexString((String) encryptedMessageJSON.get("data"));
            byte[] nonce = Convert.parseHexString((String) encryptedMessageJSON.get("nonce"));
            this.encryptedData = new EncryptedData(data, nonce);
            this.isText = Boolean.TRUE.equals(encryptedMessageJSON.get("isText"));
            this.isCompressed = Boolean.TRUE.equals(encryptedMessageJSON.get("isCompressed"));
        }
    }

    public PrunableEncryptedMessageAppendix(EncryptedData encryptedData, boolean isText, boolean isCompressed) {
        this.encryptedData = encryptedData;
        this.isText = isText;
        this.isCompressed = isCompressed;
        this.hash = null;
    }

    @Override
    public final Fee getBaselineFee(Transaction transaction) {
        return PRUNABLE_ENCRYPTED_DATA_FEE;
    }

    @Override
    final int getMySize() {
        return 32;
    }

    @Override
    final int getMyFullSize() {
        return getEncryptedDataLength();
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.put(getHash());
    }

    @Override
    void putMyJSON(JSONObject json) {
        if (prunableMessage != null) {
            JSONObject encryptedMessageJSON = new JSONObject();
            json.put("encryptedMessage", encryptedMessageJSON);
            encryptedMessageJSON.put("data", Convert.toHexString(prunableMessage.getEncryptedData().getData()));
            encryptedMessageJSON.put("nonce", Convert.toHexString(prunableMessage.getEncryptedData().getNonce()));
            encryptedMessageJSON.put("isText", prunableMessage.encryptedMessageIsText());
            encryptedMessageJSON.put("isCompressed", prunableMessage.isCompressed());
        } else if (encryptedData != null) {
            JSONObject encryptedMessageJSON = new JSONObject();
            json.put("encryptedMessage", encryptedMessageJSON);
            encryptedMessageJSON.put("data", Convert.toHexString(encryptedData.getData()));
            encryptedMessageJSON.put("nonce", Convert.toHexString(encryptedData.getNonce()));
            encryptedMessageJSON.put("isText", isText);
            encryptedMessageJSON.put("isCompressed", isCompressed);
        }
        json.put("encryptedMessageHash", Convert.toHexString(getHash()));
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (transaction.getEncryptedMessage() != null) {
            throw new AplException.NotValidException("Cannot have both encrypted and prunable encrypted message attachments");
        }
        EncryptedData ed = getEncryptedData();
        if (ed == null && timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMinPrunableLifetime()) {
            throw new AplException.NotCurrentlyValidException("Encrypted message has been pruned prematurely");
        }
        if (ed != null) {
            if (ed.getData().length > Constants.MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH) {
                throw new AplException.NotValidException(String.format("Message length %d exceeds max prunable encrypted message length %d",
                        ed.getData().length, Constants.MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH));
            }
            if ((ed.getNonce().length != 32 && ed.getData().length > 0)
                    || (ed.getNonce().length != 0 && ed.getData().length == 0)) {
                throw new AplException.NotValidException("Invalid nonce length " + ed.getNonce().length);
            }
        }
        if (transaction.getRecipientId() == 0) {
            throw new AplException.NotValidException("Encrypted messages cannot be attached to transactions with no recipient");
        }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime()) {
            PrunableMessage.add((TransactionImpl)transaction, this);
        }
    }

    public final EncryptedData getEncryptedData() {
        if (prunableMessage != null) {
            return prunableMessage.getEncryptedData();
        }
        return encryptedData;
    }

    final void setEncryptedData(EncryptedData encryptedData) {
        this.encryptedData = encryptedData;
    }

    int getEncryptedDataLength() {
        return getEncryptedData() == null ? 0 : getEncryptedData().getData().length;
    }

    public final boolean isText() {
        if (prunableMessage != null) {
            return prunableMessage.encryptedMessageIsText();
        }
        return isText;
    }

    public final boolean isCompressed() {
        if (prunableMessage != null) {
            return prunableMessage.isCompressed();
        }
        return isCompressed;
    }

    @Override
    public final byte[] getHash() {
        if (hash != null) {
            return hash;
        }
        MessageDigest digest = Crypto.sha256();
        digest.update((byte)(isText ? 1 : 0));
        digest.update((byte)(isCompressed ? 1 : 0));
        digest.update(encryptedData.getData());
        digest.update(encryptedData.getNonce());
        return digest.digest();
    }

    @Override
    public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
        if (!hasPrunableData() && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            PrunableMessage prunableMessage = PrunableMessage.getPrunableMessage(transaction.getId());
            if (prunableMessage != null && prunableMessage.getEncryptedData() != null) {
                this.prunableMessage = prunableMessage;
            }
        }
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public final boolean hasPrunableData() {
        return (prunableMessage != null || encryptedData != null);
    }

    @Override
    public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        PrunableMessage.add((TransactionImpl)transaction, this, blockTimestamp, height);
    }

}
