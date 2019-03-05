/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

public class PrunableTransaction {

    private final long id;
    private final TransactionType transactionType;
    private final boolean prunableAttachment;
    private final boolean prunablePlainMessage;
    private final boolean prunableEncryptedMessage;

    public PrunableTransaction(long id, TransactionType transactionType, boolean prunableAttachment,
                               boolean prunablePlainMessage, boolean prunableEncryptedMessage) {
        this.id = id;
        this.transactionType = transactionType;
        this.prunableAttachment = prunableAttachment;
        this.prunablePlainMessage = prunablePlainMessage;
        this.prunableEncryptedMessage = prunableEncryptedMessage;
    }

    public PrunableTransaction(long id, TransactionType transactionType, boolean prunableAttachment, boolean prunablePlainMessage) {
        this.id = id;
        this.transactionType = transactionType;
        this.prunableAttachment = prunableAttachment;
        this.prunablePlainMessage = prunablePlainMessage;
        this.prunableEncryptedMessage = false;
    }

    public long getId() {
        return id;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public boolean hasPrunableAttachment() {
        return prunableAttachment;
    }

    public boolean hasPrunablePlainMessage() {
        return prunablePlainMessage;
    }

    public boolean hasPrunableEncryptedMessage() {
        return prunableEncryptedMessage;
    }
}
