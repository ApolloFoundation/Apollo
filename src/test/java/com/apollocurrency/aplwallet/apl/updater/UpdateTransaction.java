package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.TransactionType;

import java.util.Objects;

public class UpdateTransaction extends SimpleTransactionImpl {
    private Attachment attachment;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateTransaction)) return false;
        UpdateTransaction that = (UpdateTransaction) o;
        return Objects.equals(attachment, that.attachment);
    }

    @Override
    public int hashCode() {

        return Objects.hash(attachment);
    }

    public UpdateTransaction(Attachment attachment) {
        this.attachment = attachment;
    }

    public UpdateTransaction(TransactionType type, long recipientId, long senderId, long feeATM, long amountATM, Attachment attachment) {
        super(type, recipientId, senderId, feeATM, amountATM);
        this.attachment = attachment;
    }

    public UpdateTransaction(TransactionType type, long recipientId, long senderId, long feeATM, long amountATM, long height, Attachment attachment) {
        super(type, recipientId, senderId, feeATM, amountATM, height);
        this.attachment = attachment;
    }

    @Override
    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }
}
