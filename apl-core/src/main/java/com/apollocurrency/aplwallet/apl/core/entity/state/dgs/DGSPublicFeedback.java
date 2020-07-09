/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.dgs;

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

import java.util.Objects;

public class DGSPublicFeedback extends VersionedDerivedEntity {
    private String feedback;
    private Long id;


    public DGSPublicFeedback(Long dbId, Integer height, String feedback, Long id) {
        super(dbId, height);
        this.feedback = feedback;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DGSPublicFeedback)) return false;
        if (!super.equals(o)) return false;
        DGSPublicFeedback that = (DGSPublicFeedback) o;
        return Objects.equals(feedback, that.feedback) &&
            Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), feedback, id);
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "DGSPublicFeedback{" +
            "feedback='" + feedback + '\'' +
            ", id=" + id +
            '}';
    }
}
