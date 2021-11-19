/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.model;

import java.util.Objects;

public class VersionedChildDerivedEntity extends VersionedDeletableIdDerivedEntity {
    private long parentId;

    public VersionedChildDerivedEntity(Long dbId, long parentId, long id, Integer height, boolean latest) {
        super(id, dbId, height, latest);
        this.parentId = parentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionedChildDerivedEntity)) return false;
        if (!super.equals(o)) return false;
        VersionedChildDerivedEntity that = (VersionedChildDerivedEntity) o;
        return parentId == that.parentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parentId);
    }

    @Override
    public String toString() {
        return "VersionedChildDerivedEntity{" +
                "parentId=" + parentId +
                '}';
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }
}
