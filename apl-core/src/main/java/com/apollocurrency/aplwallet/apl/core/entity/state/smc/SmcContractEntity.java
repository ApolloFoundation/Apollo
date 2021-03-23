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
public class SmcContractEntity extends VersionedDerivedEntity {
    private long address; //contract address/id
    private long owner; //owner
    private long transactionId;//originator

    private String data;
    private String contractName;
    private String args;//constructor parameters

    private String languageName;//"javascript"
    private String languageVersion;//"0.0.1"

    private String status;//ref to ContractState enum

    @Builder
    public SmcContractEntity(Long dbId, Integer height, long address, long owner, long transactionId, String data, String contractName, String args, String languageName, String languageVersion, String status) {
        super(dbId, height);
        this.address = address;
        this.owner = owner;
        this.transactionId = transactionId;
        this.data = data;
        this.contractName = contractName;
        this.args = args;
        this.languageName = languageName;
        this.languageVersion = languageVersion;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmcContractEntity entity = (SmcContractEntity) o;
        return address == entity.address && owner == entity.owner && transactionId == entity.transactionId && data.equals(entity.data) && contractName.equals(entity.contractName) && Objects.equals(args, entity.args) && languageName.equals(entity.languageName) && languageVersion.equals(entity.languageVersion) && status.equals(entity.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address, owner, transactionId, data, contractName, args, languageName, languageVersion, status);
    }
}
