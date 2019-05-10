/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.util.Objects;

public class DGSPublicFeedback {
    private String feedback;
    private Long id;
    private int height;
    private DbKey dbKey;

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public DGSPublicFeedback(String feedback, Long id, int height) {
        this.feedback = feedback;
        this.id = id;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DGSPublicFeedback)) return false;
        DGSPublicFeedback that = (DGSPublicFeedback) o;
        return Objects.equals(feedback, that.feedback) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedback, id);
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
