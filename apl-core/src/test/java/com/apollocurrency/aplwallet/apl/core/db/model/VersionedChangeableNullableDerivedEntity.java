package com.apollocurrency.aplwallet.apl.core.db.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VersionedChangeableNullableDerivedEntity extends VersionedChangeableDerivedEntity {
    private String name;
    private String description;

    public VersionedChangeableNullableDerivedEntity(Long dbId, long id, int remaining, Integer height, String name, String description, boolean latest) {
        super(dbId, id, remaining, height, latest);
        this.name = name;
        this.description = description;
    }


}
