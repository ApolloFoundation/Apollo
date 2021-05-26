/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

import java.util.Objects;

public class DerivedIdEntity extends DerivedEntity {
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DerivedIdEntity(Long dbId, Integer height, long id) {
        super(dbId, height);
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DerivedIdEntity)) return false;
        if (!super.equals(o)) return false;
        DerivedIdEntity that = (DerivedIdEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
