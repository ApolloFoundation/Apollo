/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Attachment;
import org.json.simple.JSONStreamAware;

public class CreateTransactionRequestData {
    private Attachment attachment;
    private long recipientId;
    private Account senderAccount;
    private long amountATM;
    private JSONStreamAware errorJson;
    private JSONStreamAware insufficientBalanceErrorJson;

    public CreateTransactionRequestData(Attachment attachment, long recipientId, Account senderAccount, long amountATM, JSONStreamAware insufficientBalanceErrorJson) {
        this.attachment = attachment;
        this.recipientId = recipientId;
        this.senderAccount = senderAccount;
        this.amountATM = amountATM;
        this.insufficientBalanceErrorJson = insufficientBalanceErrorJson;
    }

    public CreateTransactionRequestData(Attachment attachment, long recipientId, Account senderAccount, long amountATM) {
        this(attachment, recipientId, senderAccount, amountATM, null);
    }

    public CreateTransactionRequestData(Attachment attachment, Account senderAccount) {
        this(attachment, 0, senderAccount, 0);
    }

    public JSONStreamAware getInsufficientBalanceErrorJson() {
        return insufficientBalanceErrorJson;
    }

    public CreateTransactionRequestData(Attachment attachment, Account senderAccount, JSONStreamAware insufficientBalanceErrorJson) {
        this(attachment, 0, senderAccount, 0, insufficientBalanceErrorJson);
    }

    public CreateTransactionRequestData(JSONStreamAware errorJson) {
        this.errorJson = errorJson;
    }

    public JSONStreamAware getErrorJson() {
        return errorJson;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public Account getSenderAccount() {
        return senderAccount;
    }

    public long getAmountATM() {
        return amountATM;
    }
}
