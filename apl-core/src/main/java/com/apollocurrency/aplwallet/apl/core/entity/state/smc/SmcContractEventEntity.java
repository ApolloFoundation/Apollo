/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
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
public class SmcContractEventEntity extends DerivedEntity {
    private long id;
    private long address; //contract address/id
    private long transactionId;

    private byte[] signature;//SHA256 hash(name+idxCount); 32 bytes
    private String name;
    private byte idxCount;//indexed fields count
    private boolean anonymous;//is anonymous event

    @Builder
    public SmcContractEventEntity(Long dbId, Integer height, long id, long address, long transactionId, byte[] signature, String name, byte idxCount, boolean anonymous) {
        super(dbId, height);
        this.id = id;
        this.address = address;
        this.transactionId = transactionId;
        this.signature = signature;
        this.name = name;
        this.idxCount = idxCount;
        this.anonymous = anonymous;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmcContractEventEntity that = (SmcContractEventEntity) o;
        return id == that.id && address == that.address && transactionId == that.transactionId && idxCount == that.idxCount && anonymous == that.anonymous && Arrays.equals(signature, that.signature) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), id, address, transactionId, name, idxCount, anonymous);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
