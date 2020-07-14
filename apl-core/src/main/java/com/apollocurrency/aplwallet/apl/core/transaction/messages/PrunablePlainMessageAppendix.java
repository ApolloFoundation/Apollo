/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix.hasAppendix;

public class PrunablePlainMessageAppendix extends AbstractAppendix implements Prunable {

    private static final String appendixName = "PrunablePlainMessage";
    private static final Fee PRUNABLE_MESSAGE_FEE = new Fee.SizeBasedFee(Constants.ONE_APL / 10) {
        @Override
        public int getSize(Transaction transaction, Appendix appendix) {
            return appendix.getFullSize();
        }
    };

    private byte[] hash;
    private byte[] message;
    private boolean isText;
    private volatile PrunableMessage prunableMessage;

    public PrunablePlainMessageAppendix(ByteBuffer buffer) {
        super(buffer);
        this.hash = new byte[32];
        buffer.get(this.hash);
        this.message = null;
        this.isText = false;
    }

    public PrunablePlainMessageAppendix(JSONObject attachmentData) {
        super(attachmentData);
        String hashString = Convert.emptyToNull((String) attachmentData.get("messageHash"));
        String messageString = Convert.emptyToNull((String) attachmentData.get("message"));
        if (hashString != null && messageString == null) {
            this.hash = Convert.parseHexString(hashString);
            this.message = null;
            this.isText = false;
        } else {
            this.hash = null;
            this.isText = Boolean.TRUE.equals(attachmentData.get("messageIsText"));
            this.message = Convert.toBytes(messageString, isText);
        }
    }

    public PrunablePlainMessageAppendix(byte[] message) {
        this(message, false);
    }

    public PrunablePlainMessageAppendix(String string) {
        this(Convert.toBytes(string), true);
    }

    public PrunablePlainMessageAppendix(String string, boolean isText) {
        this(Convert.toBytes(string, isText), isText);
    }

    public PrunablePlainMessageAppendix(byte[] message, boolean isText) {
        this.message = message;
        this.isText = isText;
        this.hash = null;
    }

    public static PrunablePlainMessageAppendix parse(JSONObject attachmentData) {
        if (!hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        return new PrunablePlainMessageAppendix(attachmentData);
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return PRUNABLE_MESSAGE_FEE;
    }

    @Override
    public int getMySize() {
        return 32;
    }

    @Override
    public int getMyFullSize() {
        return getMessage() == null ? 0 : getMessage().length;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(getHash());
    }

    @Override
    public void putMyJSON(JSONObject json) {
        if (prunableMessage != null) {
            json.put("message", Convert.toString(prunableMessage.getMessage(), prunableMessage.messageIsText()));
            json.put("messageIsText", prunableMessage.messageIsText());
        } else if (message != null) {
            json.put("message", Convert.toString(message, isText));
            json.put("messageIsText", isText);
        }
        json.put("messageHash", Convert.toHexString(getHash()));
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (transaction.getMessage() != null) {
            throw new AplException.NotValidException("Cannot have both message and prunable message attachments");
        }
        byte[] msg = getMessage();
        if (msg != null && msg.length > Constants.MAX_PRUNABLE_MESSAGE_LENGTH) {
            throw new AplException.NotValidException("Invalid prunable message length: " + msg.length);
        }
        if (msg == null && lookupTimeService().getEpochTime() - transaction.getTimestamp() < lookupBlockchainConfig().getMinPrunableLifetime()) {
            throw new AplException.NotCurrentlyValidException("Message has been pruned prematurely");
        }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (lookupTimeService().getEpochTime() - transaction.getTimestamp() < lookupBlockchainConfig().getMaxPrunableLifetime()) {
            lookupMessageService().add(transaction, this);
        }
    }

    public byte[] getMessage() {
        if (prunableMessage != null) {
            return prunableMessage.getMessage();
        }
        return message;
    }

    public boolean isText() {
        if (prunableMessage != null) {
            return prunableMessage.messageIsText();
        }
        return isText;
    }

    @Override
    public byte[] getHash() {
        if (hash != null) {
            return hash;
        }
        MessageDigest digest = Crypto.sha256();
        digest.update((byte) (isText ? 1 : 0));
        digest.update(message);
        return digest.digest();
    }

    @Override
    public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
        if (!hasPrunableData() && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            PrunableMessage prunableMessage = lookupMessageService().get(transaction.getId());
            if (prunableMessage != null && prunableMessage.getMessage() != null) {
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
        return (prunableMessage != null || message != null);
    }

    @Override
    public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        lookupMessageService().add(transaction, this, blockTimestamp, height);
    }
}