/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;

public class UpdateData {
    private Transaction transaction;
    private String decryptedUrl;

    public UpdateData(Transaction transaction, String decryptedUrl) {
        this.transaction = transaction;
        this.decryptedUrl = decryptedUrl;
    }

    @Override
    public String toString() {
        return "UpdateDataHolder{" +
                "transaction=" + transaction.getJSONObject().toJSONString() +
                ", decryptedUrl='" + decryptedUrl + '\'' +
                '}';
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getDecryptedUrl() {
        return decryptedUrl;
    }

    public boolean isAutomaticUpdate() {
        return transaction.getType() != TransactionType.Update.MINOR;
    }
}
