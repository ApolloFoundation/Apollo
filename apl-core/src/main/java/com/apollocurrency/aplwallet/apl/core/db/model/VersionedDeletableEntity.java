package com.apollocurrency.aplwallet.apl.core.db.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VersionedDeletableEntity extends VersionedDerivedEntity {
    @Getter
    @Setter
    private boolean deleted;

    public VersionedDeletableEntity(Long dbId, Integer height) {
        super(dbId, height);
    }

    public VersionedDeletableEntity(ResultSet rs) throws SQLException {
        super(rs);
        deleted = rs.getBoolean("deleted");
    }

}
