/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
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
public class SmcContractState extends VersionedDerivedEntity {

    private String address;//contract address/id
    private String transactionId;

    private String method; //call constructor or method
    private String args;
    private String status; //contract status:
    private String serializedObject;

    public SmcContractState(Long dbId, Integer height,
                            String address, String transactionId, String method, String args, String status,
                            String serializedObject) {
        super(dbId, height);
        this.address = address;
        this.transactionId = transactionId;
        this.method = method;
        this.args = args;
        this.status = status;
        this.serializedObject = serializedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmcContractState that = (SmcContractState) o;
        return Objects.equals(address, that.address)
            && Objects.equals(transactionId, that.transactionId)
            && Objects.equals(method, that.method)
            && Objects.equals(args, that.args)
            && Objects.equals(status, that.status)
            && Objects.equals(serializedObject, that.serializedObject);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), address, transactionId, method, args, status, serializedObject);
        return result;
    }
}
