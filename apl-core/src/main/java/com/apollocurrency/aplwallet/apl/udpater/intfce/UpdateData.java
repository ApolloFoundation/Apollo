/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.MinorUpdate;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;

public class UpdateData {
    private UpdateAttachment attachment;
    private Long transactionId;
    private String decryptedUrl;

    public UpdateData(UpdateAttachment attachment, Long transactionId, String decryptedUrl) {
        this.attachment = attachment;
        this.transactionId = transactionId;
        this.decryptedUrl = decryptedUrl;
    }

    public boolean isAutomaticUpdate() { // update is automatic for Important and Critical update types
        return !(attachment instanceof MinorUpdate);
    }
    public UpdateAttachment getAttachment() {
        return attachment;
    }

    public void setAttachment(UpdateAttachment attachment) {
        this.attachment = attachment;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getDecryptedUrl() {
        return decryptedUrl;
    }

    public void setDecryptedUrl(String decryptedUrl) {
        this.decryptedUrl = decryptedUrl;
    }
}
