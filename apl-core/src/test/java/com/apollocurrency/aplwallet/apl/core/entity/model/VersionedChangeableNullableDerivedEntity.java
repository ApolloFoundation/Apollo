package com.apollocurrency.aplwallet.apl.core.entity.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class VersionedChangeableNullableDerivedEntity extends VersionedChangeableDerivedEntity {
    private String name;
    private String description;

    public VersionedChangeableNullableDerivedEntity(Long dbId, long id, int remaining, Integer height, String name, String description, boolean latest, boolean deleted) {
        super(dbId, id, remaining, height, latest, deleted);
        this.name = name;
        this.description = description;
    }
    @Override
    public VersionedChangeableNullableDerivedEntity clone() throws CloneNotSupportedException {
        return (VersionedChangeableNullableDerivedEntity) super.clone();
    }

    @Override
    public String toString() {
        return "VersionedChangeableNullableDerivedEntity{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", remaining=" + getRemaining() +
            ", id=" + getId() +
            ", deleted=" + isDeleted() +
            ", latest=" + isLatest() +
            ", dbId=" + getDbId() +
            ", height=" + getHeight() +
            '}';
    }
}
