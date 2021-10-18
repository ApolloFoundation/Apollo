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

import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Setter
@ToString(callSuper = true)
public class SmcContractEventEntity extends DerivedEntity {
    private long id;
    private long contract; //contract address/id
    private long transactionId;
    private String spec;
    private byte[] signature;//SHA256 hash(name+idxCount); 32 bytes
    private String name;
    private byte idxCount;//indexed fields count
    private boolean anonymous;//is anonymous event

    @Builder
    public SmcContractEventEntity(Long dbId, Integer height, long id, long contract, long transactionId, String spec, byte[] signature, String name, int idxCount, boolean anonymous) {
        super(dbId, height);
        this.id = id;
        this.contract = contract;
        this.transactionId = transactionId;
        this.spec = spec;
        this.signature = signature;
        this.name = name;
        this.idxCount = (byte) idxCount;
        this.anonymous = anonymous;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmcContractEventEntity that = (SmcContractEventEntity) o;
        return id == that.id && contract == that.contract && transactionId == that.transactionId && idxCount == that.idxCount && anonymous == that.anonymous && Arrays.equals(signature, that.signature) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), id, contract, transactionId, name, idxCount, anonymous);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    public String getLogInfo() {
        return "SmcContractEventEntity{" +
            "id=" + id +
            ", contract=" + contract +
            ", transactionId=" + transactionId +
            ", signature=" + toHex(signature) +
            ", event=" + spec + ':' + name + ':' + idxCount + ':' + anonymous +
            ", height" + getHeight() +
            "} ";
    }
}
