/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.Objects;

public class DGSFeedback {
    private DbKey dbKey;
    private Long purchaseId;
    private Integer height;
    private EncryptedData feedbackEncryptedData;

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public EncryptedData getFeedbackEncryptedData() {
        return feedbackEncryptedData;
    }

    public void setFeedbackEncryptedData(EncryptedData feedbackEncryptedData) {
        this.feedbackEncryptedData = feedbackEncryptedData;
    }

    public DGSFeedback(Long purchaseId, Integer height, EncryptedData feedbackEncryptedData) {
        this.purchaseId = purchaseId;
        this.height = height;
        this.feedbackEncryptedData = feedbackEncryptedData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DGSFeedback)) return false;
        DGSFeedback that = (DGSFeedback) o;
        return Objects.equals(purchaseId, that.purchaseId) &&
                Objects.equals(feedbackEncryptedData, that.feedbackEncryptedData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(purchaseId, feedbackEncryptedData);
    }

}
