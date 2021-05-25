/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Setter
@ToString(callSuper = true)
public class SmcContractMappingEntity extends VersionedDeletableEntity {

    private long address;//contract address/id
    private byte[] key;//complex key is a SHA256 hash; 32 bytes
    private String name;//mapping name
    private String serializedObject;

    @Builder
    public SmcContractMappingEntity(Long dbId, Integer height,
                                    long address, byte[] key, String name, String serializedObject) {
        super(dbId, height);
        this.address = address;
        this.name = name;
        this.key = key;
        this.serializedObject = serializedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmcContractMappingEntity that = (SmcContractMappingEntity) o;
        return address == that.address && name.equals(that.name) && Arrays.equals(key, that.key) && serializedObject.equals(that.serializedObject);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), address, name, serializedObject);
        result = 31 * result + Arrays.hashCode(key);
        return result;
    }
}
