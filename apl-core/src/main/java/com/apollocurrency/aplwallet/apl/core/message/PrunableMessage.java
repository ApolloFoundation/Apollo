/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.message;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.Arrays;
import java.util.Objects;

public class PrunableMessage extends DerivedEntity {
    private final long id;
    private final long senderId;
    private final long recipientId;
    private final int transactionTimestamp;
    private final int blockTimestamp;
    private byte[] message;
    private EncryptedData encryptedData;
    private boolean messageIsText;
    private boolean encryptedMessageIsText;
    private boolean isCompressed;

    public PrunableMessage(Transaction transaction, int blockTimestamp, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.blockTimestamp = blockTimestamp;
        this.transactionTimestamp = transaction.getTimestamp();
    }

    public PrunableMessage(Long dbId, long id, long senderId, long recipientId, byte[] message, EncryptedData encryptedData, boolean messageIsText, boolean encryptedMessageIsText, boolean isCompressed, int blockTimestamp, int transactionTimestamp, Integer height) {
        super(dbId, height);
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.message = message;
        this.encryptedData = encryptedData;
        this.messageIsText = messageIsText;
        this.encryptedMessageIsText = encryptedMessageIsText;
        this.isCompressed = isCompressed;
        this.transactionTimestamp = transactionTimestamp;
        this.blockTimestamp = blockTimestamp;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public EncryptedData getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(EncryptedData encryptedData) {
        this.encryptedData = encryptedData;
    }

    public boolean messageIsText() {
        return messageIsText;
    }

    public boolean encryptedMessageIsText() {
        return encryptedMessageIsText;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public void setCompressed(boolean compressed) {
        isCompressed = compressed;
    }

    public long getId() {
        return id;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    public void setPlain(PrunablePlainMessageAppendix appendix) {
        this.message = appendix.getMessage();
        this.messageIsText = appendix.isText();
    }

    public void setEncrypted(PrunableEncryptedMessageAppendix appendix) {
        this.encryptedData = appendix.getEncryptedData();
        this.encryptedMessageIsText = appendix.isText();
        this.isCompressed = appendix.isCompressed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrunableMessage)) return false;
        if (!super.equals(o)) return false;
        PrunableMessage that = (PrunableMessage) o;
        return id == that.id &&
            senderId == that.senderId &&
            recipientId == that.recipientId &&
            messageIsText == that.messageIsText &&
            encryptedMessageIsText == that.encryptedMessageIsText &&
            isCompressed == that.isCompressed &&
            transactionTimestamp == that.transactionTimestamp &&
            blockTimestamp == that.blockTimestamp &&
            Arrays.equals(message, that.message) &&
            Objects.equals(encryptedData, that.encryptedData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), id, senderId, recipientId, encryptedData, messageIsText, encryptedMessageIsText, isCompressed, transactionTimestamp, blockTimestamp);
        result = 31 * result + Arrays.hashCode(message);
        return result;
    }

    public boolean isMessageIsText() {
        return messageIsText;
    }

    public void setMessageIsText(boolean messageIsText) {
        this.messageIsText = messageIsText;
    }

    public boolean isEncryptedMessageIsText() {
        return encryptedMessageIsText;
    }

    public void setEncryptedMessageIsText(boolean encryptedMessageIsText) {
        this.encryptedMessageIsText = encryptedMessageIsText;
    }

    @Override
    public String toString() {
        return "PrunableMessage{" +
            "id=" + id +
            ", senderId=" + senderId +
            ", recipientId=" + recipientId +
            ", message=" + Arrays.toString(message) +
            ", encryptedData=" + encryptedData +
            ", messageIsText=" + messageIsText +
            ", encryptedMessageIsText=" + encryptedMessageIsText +
            ", isCompressed=" + isCompressed +
            ", transactionTimestamp=" + transactionTimestamp +
            ", blockTimestamp=" + blockTimestamp +
            '}';
    }
}
