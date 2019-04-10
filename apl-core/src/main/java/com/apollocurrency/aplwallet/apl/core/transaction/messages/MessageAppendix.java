/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public class MessageAppendix extends AbstractAppendix {

    private static final String appendixName = "Message";

    public static MessageAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        return new MessageAppendix(attachmentData);
    }

    private static final Fee MESSAGE_FEE = new Fee.SizeBasedFee(0, Constants.ONE_APL, 32) {
        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            return ((MessageAppendix)appendage).getMessage().length;
        }
    };

    private final byte[] message;
    private final boolean isText;

    public MessageAppendix(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        int messageLength = buffer.getInt();
        this.isText = messageLength < 0; // ugly hack
        if (messageLength < 0) {
            messageLength &= Integer.MAX_VALUE;
        }
        if (messageLength > 1000) {
            throw new AplException.NotValidException("Invalid arbitrary message length: " + messageLength);
        }
        this.message = new byte[messageLength];
        buffer.get(this.message);
        if (isText && !Arrays.equals(message, Convert.toBytes(Convert.toString(message)))) {
            throw new AplException.NotValidException("Message is not UTF-8 text");
        }
    }

    public MessageAppendix(JSONObject attachmentData) {
        super(attachmentData);
        String messageString = (String)attachmentData.get("message");
        this.isText = Boolean.TRUE.equals(attachmentData.get("messageIsText"));
        this.message = isText ? Convert.toBytes(messageString) : Convert.parseHexString(messageString);
    }

    public MessageAppendix(byte[] message) {
        this(message, false);
    }

    public MessageAppendix(String string) {
        this(Convert.toBytes(string), true);
    }

    public MessageAppendix(String string, boolean isText) {
        this(isText ? Convert.toBytes(string) : Convert.parseHexString(string), isText);
    }

    public MessageAppendix(byte[] message, boolean isText) {
        this.message = message;
        this.isText = isText;
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    int getMySize() {
        return 4 + message.length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putInt(isText ? (message.length | Integer.MIN_VALUE) : message.length);
        buffer.put(message);
    }

    @Override
    void putMyJSON(JSONObject json) {
        json.put("message", Convert.toString(message, isText));
        json.put("messageIsText", isText);
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return MESSAGE_FEE;
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (message.length > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
            throw new AplException.NotValidException("Invalid arbitrary message length: " + message.length);
        }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

    public byte[] getMessage() {
        return message;
    }

    public boolean isText() {
        return isText;
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public String toString() {
        return "MessageAppendix{" +
                "message=" + new String(message) +
                ", isText=" + isText +
                '}';
    }
}