/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import java.util.Objects;

public class VersionedChangeableDerivedEntity extends VersionedDerivedIdEntity {
    private int remaining;

    public VersionedChangeableDerivedEntity(Long dbId, long id, int remaining,  Integer height, boolean latest) {
        super(dbId, height, id, latest);
        this.remaining = remaining;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    @Override
    public String toString() {
        return "VersionedChangeableDerivedEntity{" +
                "remaining=" + remaining +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionedChangeableDerivedEntity)) return false;
        if (!super.equals(o)) return false;
        VersionedChangeableDerivedEntity that = (VersionedChangeableDerivedEntity) o;
        return remaining == that.remaining;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), remaining);
    }

    @Override
    public VersionedChangeableDerivedEntity clone() throws CloneNotSupportedException {
        return (VersionedChangeableDerivedEntity) super.clone();
    }
}
