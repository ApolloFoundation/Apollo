/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
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
public class SmcContractEntity extends VersionedDerivedEntity {
    private long address; //contract address/id
    private long owner; //owner
    private long transactionId;//originator
    private int blockTimestamp;
    private byte[] transactionHash;
    private long fuelPrice;
    private long fuelLimit;
    private long fuelCharged;

    private String data;
    private String contractName;
    private String baseContract;//contract type - APL20
    private String args;//constructor parameters

    private String languageName;//"javascript"
    private String languageVersion;//"0.1.1"

    private String status;//ref to ContractState enum

    @Builder
    public SmcContractEntity(Long dbId, Integer height, long address, long owner,
                             long transactionId, int blockTimestamp, byte[] transactionHash,
                             long fuelPrice, long fuelLimit, long fuelCharged,
                             String data, String contractName, String baseContract, String args,
                             String languageName, String languageVersion, String status) {
        super(dbId, height);
        this.address = address;
        this.owner = owner;
        this.transactionId = transactionId;
        this.blockTimestamp = blockTimestamp;
        this.transactionHash = transactionHash;
        this.fuelPrice = fuelPrice;
        this.fuelLimit = fuelLimit;
        this.fuelCharged = fuelCharged;
        this.data = data;
        this.contractName = contractName;
        this.baseContract = baseContract;
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
        return address == entity.address && owner == entity.owner
            && transactionId == entity.transactionId && Arrays.equals(transactionHash, entity.transactionHash)
            && data.equals(entity.data) && contractName.equals(entity.contractName) && baseContract.equals(entity.baseContract) && Objects.equals(args, entity.args)
            && languageName.equals(entity.languageName) && languageVersion.equals(entity.languageVersion) && status.equals(entity.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address, owner, transactionId, Arrays.hashCode(transactionHash), data, contractName, baseContract, args, languageName, languageVersion, status);
    }
}
