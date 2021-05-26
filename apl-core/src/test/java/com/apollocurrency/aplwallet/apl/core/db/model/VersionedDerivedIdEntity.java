/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;

import java.util.Objects;

public class VersionedDerivedIdEntity extends VersionedDerivedEntity {
    private Long id;
    public VersionedDerivedIdEntity(Long dbId, Integer height, Long id, boolean latest) {
        super(dbId, height);
        this.id = id;
        setLatest(latest);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionedDerivedIdEntity)) return false;
        if (!super.equals(o)) return false;
        VersionedDerivedIdEntity that = (VersionedDerivedIdEntity) o;
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
