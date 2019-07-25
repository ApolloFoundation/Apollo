/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.message;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

public class PrunableMessage extends DerivedEntity {
    private final long id;
    private final long senderId;
    private final long recipientId;
    private byte[] message;
    private EncryptedData encryptedData;
    private boolean messageIsText;
    private boolean encryptedMessageIsText;
    private boolean isCompressed;
    private final int transactionTimestamp;
    private final int blockTimestamp;
    public byte[] getMessage() {
        return message;
    }

    public EncryptedData getEncryptedData() {
        return encryptedData;
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

    public PrunableMessage(Transaction transaction, int blockTimestamp, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.blockTimestamp = blockTimestamp;
        this.transactionTimestamp = transaction.getTimestamp();
    }

    public PrunableMessage(Long dbId, Integer height, long id, long senderId, long recipientId, byte[] message, EncryptedData encryptedData, boolean messageIsText, boolean encryptedMessageIsText, boolean isCompressed, int transactionTimestamp, int blockTimestamp) {
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

    public void setPlain(PrunablePlainMessageAppendix appendix) {
        this.message = appendix.getMessage();
        this.messageIsText = appendix.isText();
    }

    public void setEncrypted(PrunableEncryptedMessageAppendix appendix) {
        this.encryptedData = appendix.getEncryptedData();
        this.encryptedMessageIsText = appendix.isText();
        this.isCompressed = appendix.isCompressed();
    }
}
