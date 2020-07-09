/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.dgs;

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.Objects;

public class DGSFeedback extends VersionedDerivedEntity {
    private Long purchaseId;
    private EncryptedData feedbackEncryptedData;

    public DGSFeedback(Long dbId, Integer height, Long purchaseId, EncryptedData feedbackEncryptedData) {
        super(dbId, height);
        this.purchaseId = purchaseId;
        this.feedbackEncryptedData = feedbackEncryptedData;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public EncryptedData getFeedbackEncryptedData() {
        return feedbackEncryptedData;
    }

    public void setFeedbackEncryptedData(EncryptedData feedbackEncryptedData) {
        this.feedbackEncryptedData = feedbackEncryptedData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DGSFeedback)) return false;
        if (!super.equals(o)) return false;
        DGSFeedback that = (DGSFeedback) o;
        return Objects.equals(purchaseId, that.purchaseId) &&
            Objects.equals(feedbackEncryptedData, that.feedbackEncryptedData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), purchaseId, feedbackEncryptedData);
    }
}
