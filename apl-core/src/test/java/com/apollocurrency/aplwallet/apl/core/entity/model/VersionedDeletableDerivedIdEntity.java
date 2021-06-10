/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.model;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;

import java.util.Objects;

public class VersionedDeletableDerivedIdEntity extends VersionedDeletableEntity {
    private Long id;
    public VersionedDeletableDerivedIdEntity(Long dbId, Integer height, Long id, boolean latest, boolean deleted) {
        super(dbId, height);
        this.id = id;
        setLatest(latest);
        setDeleted(deleted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionedDeletableDerivedIdEntity)) return false;
        if (!super.equals(o)) return false;
        VersionedDeletableDerivedIdEntity that = (VersionedDeletableDerivedIdEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "VersionedDerivedIdEntity{" +
                "id=" + id +
                '}';
    }
}
