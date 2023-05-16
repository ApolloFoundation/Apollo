/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Setter
@ToString(callSuper = true)
public class SmcContractStateEntity extends VersionedDerivedEntity {

    private long address;//contract address/id
    private String serializedObject;
    private String status; //contract status:

    @Builder
    public SmcContractStateEntity(Long dbId, Integer height,
                                  long address, String serializedObject, String status) {
        super(dbId, height);
        this.address = address;
        this.status = status;
        this.serializedObject = serializedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmcContractStateEntity that = (SmcContractStateEntity) o;
        return Objects.equals(address, that.address)
            && Objects.equals(status, that.status)
            && Objects.equals(serializedObject, that.serializedObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address, status, serializedObject);
    }
}
