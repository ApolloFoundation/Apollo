/*
 * Copyright © 2018 Apollo Foundation
 */

package dto.transaction;

import java.util.Objects;

import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.TransactionType;

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
        this(0, type, recipientId, senderId, feeATM, amountATM, 0, attachment);
    }

    public UpdateTransaction(long id, TransactionType type, long recipientId, long senderId, long feeATM, long amountATM, long height,
                             Attachment attachment) {
        super(id, type, recipientId, senderId, feeATM, amountATM, height);
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
