/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class VersionedDeletableIdDerivedEntity extends VersionedDeletableEntity {
    private long id;

    public VersionedDeletableIdDerivedEntity(long id, Long dbId, Integer height, boolean latest) {
        super(dbId, height);
        this.id = id;
        setLatest(latest);
    }

    @Override
    public String toString() {
        return "VersionedDeletableIdDerivedEntity{" +
            "id=" + id +
            ",deleted=" + isDeleted() +
            ",latest=" + isLatest() +
            ",dbId=" + getDbId() +
            ",height=" + getHeight() +
            '}';
    }
}
