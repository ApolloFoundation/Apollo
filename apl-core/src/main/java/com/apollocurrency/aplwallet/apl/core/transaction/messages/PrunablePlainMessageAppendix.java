/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix.hasAppendix;

public class PrunablePlainMessageAppendix extends AbstractAppendix implements Prunable {

    static final String APPENDIX_NAME = "PrunablePlainMessage";
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
        if (!hasAppendix(APPENDIX_NAME, attachmentData)) {
            return null;
        }
        return new PrunablePlainMessageAppendix(attachmentData);
    }

    @Override
    public String getAppendixName() {
        return APPENDIX_NAME;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction, long oneAPL) {
        return new Fee.SizeBasedFee(oneAPL / 10) {
            @Override
            public int getSize(Transaction transaction, Appendix appendix) {
                return appendix.getFullSize();
            }
        };
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

    public void setPrunableMessage(PrunableMessage prunableMessage) {
        this.prunableMessage = prunableMessage;
    }

    @Override
    public void performStateDependentValidation(Transaction transaction, int blockHeight) {
        throw new UnsupportedOperationException("Validation for prunable plain message is not supported, use separate class");
    }

    @Override
    public void performStateIndependentValidation(Transaction transaction, int blockHeight) {
        throw new UnsupportedOperationException("Validation for message appendix is not supported, use separate class");
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        throw new UnsupportedOperationException("Apply for this prunable plain appendix is not supported, use separate class");
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
    public boolean isPhasable() {
        return false;
    }

    @Override
    public final boolean hasPrunableData() {
        return (prunableMessage != null || message != null);
    }

    @Override
    public int getAppendixFlag() {
        return 0x20;
    }

}